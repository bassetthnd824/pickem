package com.curleesoft.pickem.backend.repository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Pick;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.Transaction;

@Repository
public class PickRepository extends BaseRepository<Pick> {

    public static final String COLLECTION = "picks";

    /**
     * One document per user and matchup. A repeat save overwrites this id instead
     * of inserting a second row.
     */
    public static String documentId(String userId, String matchupId) {
        return sanitize(userId) + "__" + sanitize(matchupId);
    }

    public PickRepository(Firestore firestore, Clock clock, AuditActorResolver auditActorResolver) {
        super(firestore, clock, auditActorResolver, Pick.class, COLLECTION);
    }

    /**
     * Single-field equality so this does not need the composite indexes owned by
     * US-39. Callers filter season and week in memory.
     */
    public List<Pick> findByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }

        return query(collection -> collection.whereEqualTo("userId", userId));
    }

    /**
     * Inserts, updates, and deletes one user's week of picks in one transaction.
     * {@code guard} sees the week reread inside that transaction and runs before
     * any write, so a concurrent save cannot commit a second copy of a rank.
     * Versions follow {@link BaseRepository#stage}. A stale submitted version is
     * rejected. The caller's objects stay as submitted; the returned drafts carry
     * the committed versions.
     */
    public List<Pick> commitWeek(String userId, String seasonWeekId, List<Pick> upserts, List<Pick> deletes,
            WeekGuard guard) {
        List<Pick> sources = upserts == null ? List.of() : List.copyOf(upserts);
        List<Pick> removals = deletes == null ? List.of() : deletes.stream()
                .filter(pick -> pick != null && StringUtils.hasText(pick.getId())).toList();

        if (sources.isEmpty() && removals.isEmpty()) {
            return List.of();
        }

        String actor = currentActor();
        return inTransaction(transaction -> writeWeek(transaction, userId, seasonWeekId, sources, removals, actor,
                guard));
    }

    private List<Pick> writeWeek(Transaction transaction, String userId, String seasonWeekId, List<Pick> sources,
            List<Pick> removals, String actor, WeekGuard guard) {
        Map<String, Pick> stored = readWeek(transaction, userId, seasonWeekId);

        if (guard != null) {
            guard.check(stored);
        }

        List<Pick> drafts = new ArrayList<>(sources.size());

        for (Pick source : sources) {
            drafts.add(stage(transaction, source, actor));
        }

        List<DocumentReference> deleted = new ArrayList<>();

        for (Pick removal : removals) {
            Optional<Pick> current = readInTransaction(transaction, removal.getId());

            if (current.isEmpty()) {
                continue;
            }

            String removalId = Objects.requireNonNull(removal.getId(), "id");
            assertVersion(current.get(), removal.getVersion(), removalId);
            deleted.add(Objects.requireNonNull(collection().document(removalId), "document"));
        }

        for (Pick draft : drafts) {
            String id = Objects.requireNonNull(draft.getId(), "id");
            DocumentReference reference = Objects.requireNonNull(collection().document(id), "document");
            transaction.set(reference, draft);
        }

        for (DocumentReference ref : deleted) {
            transaction.delete(Objects.requireNonNull(ref, "document"));
        }

        return drafts;
    }

    private Map<String, Pick> readWeek(Transaction transaction, String userId, String seasonWeekId) {
        Map<String, Pick> stored = new HashMap<>();

        if (!StringUtils.hasText(userId)) {
            return stored;
        }

        for (QueryDocumentSnapshot document : TransactionReads.get(transaction, collection().whereEqualTo("userId", userId))
                .getDocuments()) {
            Pick pick = document.toObject(Pick.class);

            if (pick == null || !seasonWeekId.equals(pick.getSeasonWeekId()) || !StringUtils.hasText(pick.getMatchupId())) {
                continue;
            }

            stored.putIfAbsent(pick.getMatchupId(), pick);
        }

        return stored;
    }

    /**
     * Rank check for one week. Throw to abort the commit. The map is the user's
     * stored picks for that week, including rows this commit will replace or delete.
     */
    @FunctionalInterface
    public interface WeekGuard {
        void check(Map<String, Pick> storedByMatchupId);
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace("/", "_");
    }
}
