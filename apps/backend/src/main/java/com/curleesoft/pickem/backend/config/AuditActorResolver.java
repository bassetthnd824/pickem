package com.curleesoft.pickem.backend.config;

/**
 * Supplies the actor stamped onto {@code createUser} / {@code lastUpdateUser}.
 * US-04 will resolve the Firebase uid from the session cookie; until then
 * unauthenticated writes use {@link #SYSTEM_ACTOR}.
 */
@FunctionalInterface
public interface AuditActorResolver {
  String SYSTEM_ACTOR = "system";

  String currentActor();
}
