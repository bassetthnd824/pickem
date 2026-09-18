package com.curleesoft.pickem.backend.repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Team;
import com.google.cloud.firestore.Firestore;
import java.time.Clock;
import org.springframework.stereotype.Repository;

@Repository
public class TeamRepository extends BaseRepository<Team> {

  public static final String COLLECTION = "teams";

  public TeamRepository(
    Firestore firestore,
    Clock clock,
    AuditActorResolver auditActorResolver
  ) {
    super(firestore, clock, auditActorResolver, Team.class, COLLECTION);
  }
}
