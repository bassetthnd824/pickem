package com.curleesoft.pickem.backend.repository;

import java.time.Clock;

import org.springframework.stereotype.Repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.ImportJob;
import com.google.cloud.firestore.Firestore;

@Repository
public class ImportJobRepository extends BaseRepository<ImportJob> {

    public static final String COLLECTION = "importJobs";

    public ImportJobRepository(Firestore firestore, Clock clock, AuditActorResolver auditActorResolver) {
        super(firestore, clock, auditActorResolver, ImportJob.class, COLLECTION);
    }
}
