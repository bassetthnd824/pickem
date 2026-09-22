package com.curleesoft.pickem.backend.security;

import java.time.Duration;
import java.util.Map;

/**
 * Firebase Admin operations used to mint and check the {@code __session}
 * cookie. Tests supply a fake; production talks to Firebase Auth.
 */
public interface FirebaseIdentityClient {

    VerifiedIdentity verifyIdToken(String idToken);

    /**
     * Mints a session cookie. Firebase copies claims from {@code idToken}; custom
     * claims set on the user after that token was issued are not included.
     */
    String createSessionCookie(String idToken, Duration expiresIn);

    /**
     * Verifies a session cookie with revocation checking
     * ({@code checkRevoked=true}).
     */
    VerifiedIdentity verifySessionCookie(String sessionCookie);

    void setCustomUserClaims(String uid, Map<String, Object> claims);

    void revokeRefreshTokens(String uid);
}
