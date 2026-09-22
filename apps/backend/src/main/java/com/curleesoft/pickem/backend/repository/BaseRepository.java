package com.curleesoft.pickem.backend.repository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

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
     * rejects a stale {@code version}.
     */
    public D save(D document, String actor) {
        Objects.requireNonNull(document, "document");
        String resolvedActor = StringUtils.hasText(actor) ? actor : AuditActorResolver.SYSTEM_ACTOR;

        try {
            return firestore.<D>runTransaction(transaction -> {
                Instant now = clock.instant();
                DocumentReference ref;
                boolean insert;
                D existing = null;

                String documentId = document.getId();

                if (documentId == null || documentId.isBlank()) {
                    ref = collection().document();
                    document.setId(ref.getId());
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

                if (insert) {
                    document.setCreateDate(now);
                    document.setCreateUser(resolvedActor);
                    document.setLastUpdateDate(now);
                    document.setLastUpdateUser(resolvedActor);
                    document.setVersion(0L);

                } else {
                    if (existing == null) {
                        throw new FirestoreAccessException("Failed to map " + collectionName + "/" + document.getId(),
                                null);
                    }

                    Long storedVersion = existing.getVersion();

                    if (storedVersion == null) {
                        storedVersion = 0L;
                    }

                    if (document.getVersion() == null || !storedVersion.equals(document.getVersion())) {
                        throw new StaleDocumentVersionException(collectionName, document.getId(), storedVersion,
                                document.getVersion());
                    }

                    document.setCreateDate(existing.getCreateDate());
                    document.setCreateUser(existing.getCreateUser());
                    document.setLastUpdateDate(now);
                    document.setLastUpdateUser(resolvedActor);
                    document.setVersion(storedVersion + 1);
                }

                transaction.set(ref, document);
                return document;
            }).get();

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
}
