package com.curleesoft.pickem.backend.repository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Pick;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;

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
     * Inserts, updates, and deletes one user's week of picks in a single Firestore
     * batched write. A document with no create date is an insert (version 0),
     * including one whose id was chosen with {@link #documentId}. An update keeps
     * that id and the create audit, and increments {@code version}.
     */
    public List<Pick> commitWeek(List<Pick> upserts, List<String> deleteIds) {
        List<Pick> sources = upserts == null ? List.of() : upserts;
        List<String> removals = new ArrayList<>();

        if (deleteIds != null) {
            for (String id : deleteIds) {
                if (StringUtils.hasText(id)) {
                    removals.add(id);
                }
            }
        }

        if (sources.isEmpty() && removals.isEmpty()) {
            return List.of();
        }

        String actor = currentActor();
        Instant now = clock().instant();
        List<Pick> drafts = new ArrayList<>(sources.size());

        for (Pick source : sources) {
            drafts.add(stamp(source, actor, now));
        }

        WriteBatch batch = firestore().batch();

        for (Pick draft : drafts) {
            batch.set(collection().document(draft.getId()), draft);
        }

        for (String id : removals) {
            batch.delete(collection().document(id));
        }

        try {
            batch.commit().get();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted saving " + COLLECTION, ex);

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new FirestoreAccessException("Failed to save " + COLLECTION, cause);
        }

        return drafts;
    }

    private Pick stamp(Pick source, String actor, Instant now) {
        Pick draft = draftOf(source);

        if (draft.getCreateDate() == null) {
            if (!StringUtils.hasText(draft.getId())) {
                draft.setId(collection().document().getId());
            }

            draft.setCreateDate(now);
            draft.setCreateUser(actor);
            draft.setVersion(0L);

        } else {
            long stored = draft.getVersion() == null ? 0L : draft.getVersion();
            draft.setVersion(stored + 1);
        }

        draft.setLastUpdateDate(now);
        draft.setLastUpdateUser(actor);
        return draft;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace("/", "_");
    }
}
