package com.curleesoft.pickem.backend.repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Rivalry;
import com.google.cloud.firestore.Firestore;
import java.time.Clock;
import org.springframework.stereotype.Repository;

@Repository
public class RivalryRepository extends BaseRepository<Rivalry> {

  public static final String COLLECTION = "rivalries";

  public RivalryRepository(
    Firestore firestore,
    Clock clock,
    AuditActorResolver auditActorResolver
  ) {
    super(firestore, clock, auditActorResolver, Rivalry.class, COLLECTION);
  }
}
