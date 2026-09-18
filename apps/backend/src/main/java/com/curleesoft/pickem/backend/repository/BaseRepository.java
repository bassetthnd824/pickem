package com.curleesoft.pickem.backend.repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.AuditableDocument;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.springframework.util.StringUtils;

/**
 * Shared Firestore DAO. Ports {@code GenericHibernateBean} + the audit/version
 * behavior of {@code AbstractBaseEntity}.
 */
public class BaseRepository<T extends AuditableDocument> {

  private final Firestore firestore;
  private final Clock clock;
  private final AuditActorResolver auditActorResolver;
  private final Class<T> type;
  private final String collectionName;

  public BaseRepository(
    Firestore firestore,
    Clock clock,
    AuditActorResolver auditActorResolver,
    Class<T> type,
    String collectionName
  ) {
    this.firestore = firestore;
    this.clock = clock;
    this.auditActorResolver = auditActorResolver;
    this.type = type;
    this.collectionName = collectionName;
  }

  public Optional<T> findById(String id) {
    if (!StringUtils.hasText(id)) {
      return Optional.empty();
    }
    try {
      DocumentSnapshot snapshot = collection().document(id).get().get();
      if (!snapshot.exists()) {
        return Optional.empty();
      }
      return Optional.ofNullable(snapshot.toObject(type));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new FirestoreAccessException(
        "Interrupted reading " + collectionName + "/" + id,
        ex
      );
    } catch (ExecutionException ex) {
      throw new FirestoreAccessException(
        "Failed to read " + collectionName + "/" + id,
        ex.getCause()
      );
    }
  }

  public List<T> findAll() {
    return query(collection -> collection);
  }

  public List<T> query(Function<CollectionReference, Query> queryFactory) {
    Objects.requireNonNull(queryFactory, "queryFactory");
    try {
      QuerySnapshot snapshot = queryFactory.apply(collection()).get().get();
      List<T> results = new ArrayList<>(snapshot.size());
      for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
        T mapped = document.toObject(type);
        if (mapped != null) {
          results.add(mapped);
        }
      }
      return results;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new FirestoreAccessException(
        "Interrupted querying " + collectionName,
        ex
      );
    } catch (ExecutionException ex) {
      throw new FirestoreAccessException(
        "Failed to query " + collectionName,
        ex.getCause()
      );
    }
  }

  public T save(T document) {
    return save(document, auditActorResolver.currentActor());
  }

  /**
   * Inserts or updates {@code document} in a transaction. Stamps audit fields
   * and rejects a stale {@code version}.
   */
  public T save(T document, String actor) {
    Objects.requireNonNull(document, "document");
    String resolvedActor = StringUtils.hasText(actor)
      ? actor
      : AuditActorResolver.SYSTEM_ACTOR;
    try {
      return firestore
        .runTransaction(transaction -> {
          Instant now = clock.instant();
          DocumentReference ref;
          boolean insert;
          T existing = null;

          if (!StringUtils.hasText(document.getId())) {
            ref = collection().document();
            document.setId(ref.getId());
            insert = true;
          } else {
            ref = collection().document(document.getId());
            DocumentSnapshot snapshot = transaction.get(ref).get();
            if (!snapshot.exists()) {
              insert = true;
            } else {
              insert = false;
              existing = snapshot.toObject(type);
            }
          }

          if (insert) {
            document.setCreateDate(now);
            document.setCreateUser(resolvedActor);
            document.setLastUpdateDate(now);
            document.setLastUpdateUser(resolvedActor);
            document.setVersion(0L);
          } else {
            Long storedVersion = existing == null ? null : existing.getVersion();
            if (storedVersion == null) {
              storedVersion = 0L;
            }
            if (
              document.getVersion() == null ||
              !storedVersion.equals(document.getVersion())
            ) {
              throw new StaleDocumentVersionException(
                collectionName,
                document.getId(),
                storedVersion,
                document.getVersion()
              );
            }
            document.setCreateDate(existing.getCreateDate());
            document.setCreateUser(existing.getCreateUser());
            document.setLastUpdateDate(now);
            document.setLastUpdateUser(resolvedActor);
            document.setVersion(storedVersion + 1);
          }

          transaction.set(ref, document);
          return document;
        })
        .get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new FirestoreAccessException(
        "Interrupted saving " + collectionName,
        ex
      );
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof StaleDocumentVersionException stale) {
        throw stale;
      }
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      throw new FirestoreAccessException(
        "Failed to save " + collectionName,
        cause
      );
    }
  }

  public void delete(String id) {
    if (!StringUtils.hasText(id)) {
      throw new IllegalArgumentException("id is required");
    }
    try {
      collection().document(id).delete().get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new FirestoreAccessException(
        "Interrupted deleting " + collectionName + "/" + id,
        ex
      );
    } catch (ExecutionException ex) {
      throw new FirestoreAccessException(
        "Failed to delete " + collectionName + "/" + id,
        ex.getCause()
      );
    }
  }

  protected CollectionReference collection() {
    return firestore.collection(collectionName);
  }

  protected String collectionName() {
    return collectionName;
  }
}
