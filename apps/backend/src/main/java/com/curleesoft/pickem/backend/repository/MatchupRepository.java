package com.curleesoft.pickem.backend.repository;

import java.time.Clock;

import org.springframework.stereotype.Repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Matchup;
import com.google.cloud.firestore.Firestore;

@Repository
public class MatchupRepository extends BaseRepository<Matchup> {

    public static final String COLLECTION = "matchups";

    public MatchupRepository(Firestore firestore, Clock clock, AuditActorResolver auditActorResolver) {
        super(firestore, clock, auditActorResolver, Matchup.class, COLLECTION);
    }
}
