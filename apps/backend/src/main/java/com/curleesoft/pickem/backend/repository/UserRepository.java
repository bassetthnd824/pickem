package com.curleesoft.pickem.backend.repository;

import java.time.Clock;

import org.springframework.stereotype.Repository;

import com.curleesoft.pickem.backend.config.AuditActorResolver;
import com.curleesoft.pickem.backend.model.User;
import com.google.cloud.firestore.Firestore;

@Repository
public class UserRepository extends BaseRepository<User> {

    public static final String COLLECTION = "users";

    public UserRepository(Firestore firestore, Clock clock, AuditActorResolver auditActorResolver) {
        super(firestore, clock, auditActorResolver, User.class, COLLECTION);
    }
}
