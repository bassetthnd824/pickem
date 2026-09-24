package com.curleesoft.pickem.backend.repository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.AuditableDocument;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;

/**
 * Shared Firestore DAO. Ports {@code GenericHibernateBean} + the audit/version
 * behavior of {@code AbstractBaseEntity}. The type parameter is {@code D} so it
 * does not clash with {@code Firestore.runTransaction}'s {@code T}.
 */
public class BaseRepository<D extends AuditableDocument> {

    private final Firestore firestore;
    private final Clock clock;
    private final AuditActorResolver auditActorResolver;
    private final Class<D> type;
    private final String collectionName;

    public BaseRepository(Firestore firestore, Clock clock, AuditActorResolver auditActorResolver, Class<D> type,
            String collectionName) {
        this.firestore = firestore;
        this.clock = clock;
        this.auditActorResolver = auditActorResolver;
        this.type = type;
        this.collectionName = collectionName;
    }

    public Optional<D> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        try {
            DocumentSnapshot snapshot = collection().document(id).get().get();

            if (!snapshot.exists()) {
                return Optional.empty();
            }

            return Optional.ofNullable(toDocument(snapshot));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted reading " + collectionName + "/" + id, ex);

        } catch (ExecutionException ex) {
            throw wrap("Failed to read " + collectionName + "/" + id, ex);
        }
    }

    public List<D> findAll() {
        return query(collection -> collection);
    }

    public List<D> query(Function<CollectionReference, Query> queryFactory) {
        Objects.requireNonNull(queryFactory, "queryFactory");

        try {
            Query query = Objects.requireNonNull(queryFactory.apply(collection()), "query");

            QuerySnapshot snapshot = query.get().get();
            List<D> results = new ArrayList<>(snapshot.size());

            for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
                D mapped = toDocument(document);

                if (mapped != null) {
                    results.add(mapped);
                }
            }

            return results;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted querying " + collectionName, ex);

        } catch (ExecutionException ex) {
            throw wrap("Failed to query " + collectionName, ex);
        }
    }

    public D save(D document) {
        return save(document, auditActorResolver.currentActor());
    }

    /**
     * Inserts or updates {@code document} in a transaction. Stamps audit fields and
     * rejects a stale {@code version}. {@code constraints} run in that same
     * transaction, before the write.
     * <p>
     * Firestore may invoke the callback again after a retryable commit failure.
     * Each attempt copies {@code document} and the caller's instance is updated
     * only after the commit succeeds, so a retry still sees the submitted version.
     */
    public D save(D document, String actor, SaveConstraint... constraints) {
        Objects.requireNonNull(document, "document");
        String resolvedActor = StringUtils.hasText(actor) ? actor : AuditActorResolver.SYSTEM_ACTOR;
        SaveConstraint[] checks = constraints == null ? new SaveConstraint[0] : constraints;

        try {
            D committed = firestore.<D>runTransaction(transaction -> attempt(transaction, document, resolvedActor, checks))
                    .get();
            BeanUtils.copyProperties(committed, document);
            return document;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted saving " + collectionName, ex);

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

            if (cause instanceof StaleDocumentVersionException stale) {
                throw stale;
            }

            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }

            throw new FirestoreAccessException("Failed to save " + collectionName, cause);
        }
    }

    public D save(D document, SaveConstraint... constraints) {
        return save(document, auditActorResolver.currentActor(), constraints);
    }

    private D attempt(Transaction transaction, D source, String actor, SaveConstraint[] checks) {
        try {
            D draft = draftOf(source);
            Instant now = clock.instant();
            DocumentReference ref;
            boolean insert;
            D existing = null;
            String documentId = draft.getId();

            if (documentId == null || documentId.isBlank()) {
                ref = collection().document();
                documentId = ref.getId();
                draft.setId(documentId);
                insert = true;

            } else {
                ref = collection().document(documentId);
                DocumentSnapshot snapshot = transaction.get(ref).get();

                if (!snapshot.exists()) {
                    insert = true;
                } else {
                    insert = false;
                    existing = toDocument(snapshot);
                }
            }

            for (SaveConstraint check : checks) {
                if (check != null) {
                    check.check(transaction, collection(), documentId);
                }
            }

            if (insert) {
                draft.setCreateDate(now);
                draft.setCreateUser(actor);
                draft.setLastUpdateDate(now);
                draft.setLastUpdateUser(actor);
                draft.setVersion(0L);

            } else {
                if (existing == null) {
                    throw new FirestoreAccessException("Failed to map " + collectionName + "/" + documentId, null);
                }

                Long storedVersion = existing.getVersion();

                if (storedVersion == null) {
                    storedVersion = 0L;
                }

                if (draft.getVersion() == null || !storedVersion.equals(draft.getVersion())) {
                    throw new StaleDocumentVersionException(collectionName, documentId, storedVersion, draft.getVersion());
                }

                draft.setCreateDate(existing.getCreateDate());
                draft.setCreateUser(existing.getCreateUser());
                draft.setLastUpdateDate(now);
                draft.setLastUpdateUser(actor);
                draft.setVersion(storedVersion + 1);
            }

            transaction.set(ref, draft);
            return draft;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted saving " + collectionName, ex);

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }

            throw new FirestoreAccessException("Failed to save " + collectionName, cause);
        }
    }

    private D draftOf(D source) {
        try {
            D draft = type.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, draft);
            return draft;

        } catch (ReflectiveOperationException ex) {
            throw new FirestoreAccessException("Failed to copy " + collectionName, ex);
        }
    }

    public void delete(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }

        try {
            collection().document(id).delete().get();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted deleting " + collectionName + "/" + id, ex);

        } catch (ExecutionException ex) {
            throw wrap("Failed to delete " + collectionName + "/" + id, ex);
        }
    }

    protected CollectionReference collection() {
        String name = collectionName;

        if (name == null) {
            throw new IllegalStateException("collection name is required");
        }

        return firestore.collection(name);
    }

    private D toDocument(DocumentSnapshot snapshot) {
        Class<D> documentType = type;

        if (documentType == null) {
            throw new IllegalStateException("document type is required");
        }

        return snapshot.toObject(documentType);
    }

    private FirestoreAccessException wrap(String message, ExecutionException ex) {
        Throwable cause = ex.getCause();
        return new FirestoreAccessException(message, cause != null ? cause : ex);
    }

    /**
     * Read-only check inside {@link #save}. Throw to reject the write. Reads must
     * use {@code transaction} so they participate in the commit.
     */
    @FunctionalInterface
    public interface SaveConstraint {
        void check(Transaction transaction, CollectionReference collection, String documentId);
    }
}
