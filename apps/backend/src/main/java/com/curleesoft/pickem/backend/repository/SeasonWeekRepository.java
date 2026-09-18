package com.curleesoft.pickem.backend.repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.google.cloud.firestore.Firestore;
import java.time.Clock;
import org.springframework.stereotype.Repository;

@Repository
public class SeasonWeekRepository extends BaseRepository<SeasonWeek> {

  public static final String COLLECTION = "seasonWeeks";

  public SeasonWeekRepository(
    Firestore firestore,
    Clock clock,
    AuditActorResolver auditActorResolver
  ) {
    super(firestore, clock, auditActorResolver, SeasonWeek.class, COLLECTION);
  }
}
