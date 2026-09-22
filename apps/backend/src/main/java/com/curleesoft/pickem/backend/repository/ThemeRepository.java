package com.curleesoft.pickem.backend.repository;

import java.time.Clock;

import org.springframework.stereotype.Repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.Theme;
import com.google.cloud.firestore.Firestore;

@Repository
public class ThemeRepository extends BaseRepository<Theme> {

    public static final String COLLECTION = "themes";

    public ThemeRepository(Firestore firestore, Clock clock, AuditActorResolver auditActorResolver) {
        super(firestore, clock, auditActorResolver, Theme.class, COLLECTION);
    }
}
