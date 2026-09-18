package com.curleesoft.pickem.backend.repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Pick;
import com.google.cloud.firestore.Firestore;
import java.time.Clock;
import org.springframework.stereotype.Repository;

@Repository
public class PickRepository extends BaseRepository<Pick> {

  public static final String COLLECTION = "picks";

  public PickRepository(
    Firestore firestore,
    Clock clock,
    AuditActorResolver auditActorResolver
  ) {
    super(firestore, clock, auditActorResolver, Pick.class, COLLECTION);
  }
}
