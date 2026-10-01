package com.curleesoft.pickem.backend.seed;

import java.util.Optional;

/**
 * Looks up a Firebase Auth user by email and replaces that user's custom
 * claims. The seed never creates a password user.
 */
public interface BootstrapIdentity {

    Optional<BootstrapAccount> findByEmail(String email);

    void grantManagerAndPlayer(String uid);
}
