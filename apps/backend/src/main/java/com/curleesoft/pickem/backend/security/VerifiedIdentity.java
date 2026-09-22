package com.curleesoft.pickem.backend.security;

import java.util.Map;

/**
 * Firebase user proven by an ID token or a session cookie. {@code claims} are
 * the token claims, including custom claims when the token was minted after
 * {@code setCustomUserClaims}.
 */
public record VerifiedIdentity(String uid, String email, String displayName, String givenName, String familyName,
        String signInProvider, Map<String, Object> claims) {
    public VerifiedIdentity {
        uid = uid == null ? "" : uid;
        email = email == null ? "" : email;
        displayName = displayName == null ? "" : displayName;
        givenName = givenName == null ? "" : givenName;
        familyName = familyName == null ? "" : familyName;
        signInProvider = signInProvider == null ? "" : signInProvider;
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }
}
