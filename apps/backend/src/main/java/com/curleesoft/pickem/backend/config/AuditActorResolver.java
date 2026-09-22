package com.curleesoft.pickem.backend.config;

/**
 * Supplies the actor stamped onto {@code createUser} / {@code lastUpdateUser}.
 * The session filter sets the Firebase uid as the authentication name.
 * Unauthenticated writes use {@link #SYSTEM_ACTOR}.
 */
@FunctionalInterface
public interface AuditActorResolver {
    String SYSTEM_ACTOR = "system";

    String currentActor();
}
