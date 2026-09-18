package com.curleesoft.pickem.backend.repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Venue;
import com.google.cloud.firestore.Firestore;
import java.time.Clock;
import org.springframework.stereotype.Repository;

@Repository
public class VenueRepository extends BaseRepository<Venue> {

  public static final String COLLECTION = "venues";

  public VenueRepository(
    Firestore firestore,
    Clock clock,
    AuditActorResolver auditActorResolver
  ) {
    super(firestore, clock, auditActorResolver, Venue.class, COLLECTION);
  }
}
