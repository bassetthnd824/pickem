package com.curleesoft.pickem.backend.repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Season;
import com.google.cloud.firestore.Firestore;
import java.time.Clock;
import org.springframework.stereotype.Repository;

@Repository
public class SeasonRepository extends BaseRepository<Season> {

  public static final String COLLECTION = "seasons";

  public SeasonRepository(
    Firestore firestore,
    Clock clock,
    AuditActorResolver auditActorResolver
  ) {
    super(firestore, clock, auditActorResolver, Season.class, COLLECTION);
  }
}
