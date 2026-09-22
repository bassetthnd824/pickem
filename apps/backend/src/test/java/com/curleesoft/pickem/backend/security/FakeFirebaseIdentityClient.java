package com.curleesoft.pickem.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory Firebase Auth. Session cookies keep the ID token's claims, matching
 * {@code createSessionCookie}. Revocation rejects cookies issued at or before
 * {@code revokeRefreshTokens}.
 */
@Component
@ConditionalOnProperty(prefix = "pickem.auth", name = "provider", havingValue = "fake")
public class FakeFirebaseIdentityClient implements FirebaseIdentityClient {

    private final Clock clock;

    private final Map<String, VerifiedIdentity> idTokens = new ConcurrentHashMap<>();

    private final Map<String, IssuedSession> sessions = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Object>> customClaims = new ConcurrentHashMap<>();

    private final Map<String, Integer> claimWrites = new ConcurrentHashMap<>();

    private final Map<String, Instant> revokedAt = new ConcurrentHashMap<>();

    public FakeFirebaseIdentityClient(Clock clock) {
        this.clock = clock;
    }

    public void registerIdToken(String idToken, VerifiedIdentity identity) {
        idTokens.put(idToken, identity);
    }

    public String issueCookie(VerifiedIdentity identity) {
        String cookie = newCookie();
        sessions.put(cookie, new IssuedSession(identity, clock.instant()));
        return cookie;
    }

    public Optional<VerifiedIdentity> identityForCookie(String sessionCookie) {
        IssuedSession issued = sessions.get(sessionCookie);
        if (issued == null) {
            return Optional.empty();
        }
        return Optional.of(issued.identity());
    }

    public Map<String, Object> customClaims(String uid) {
        return customClaims.getOrDefault(uid, Map.of());
    }

    public int claimWriteCount(String uid) {
        return claimWrites.getOrDefault(uid, 0);
    }

    public void clear() {
        idTokens.clear();
        sessions.clear();
        customClaims.clear();
        claimWrites.clear();
        revokedAt.clear();
    }

    @Override
    public VerifiedIdentity verifyIdToken(String idToken) {
        VerifiedIdentity identity = idTokens.get(idToken);
        if (identity == null) {
            throw new InvalidCredentialException("Invalid Google ID token");
        }
        return identity;
    }

    @Override
    public String createSessionCookie(String idToken, Duration expiresIn) {
        VerifiedIdentity identity = verifyIdToken(idToken);
        if (expiresIn.compareTo(Duration.ofMinutes(5)) < 0) {
            throw new InvalidCredentialException("Session duration is too short");
        }
        String cookie = newCookie();
        sessions.put(cookie, new IssuedSession(identity, clock.instant()));
        return cookie;
    }

    @Override
    public VerifiedIdentity verifySessionCookie(String sessionCookie) {
        IssuedSession issued = sessions.get(sessionCookie);
        if (issued == null) {
            throw new InvalidCredentialException("Invalid or revoked session cookie");
        }
        Instant revoked = revokedAt.get(issued.identity().uid());
        if (revoked != null && !issued.issuedAt().isAfter(revoked)) {
            throw new InvalidCredentialException("Invalid or revoked session cookie");
        }
        return issued.identity();
    }

    @Override
    public void setCustomUserClaims(String uid, Map<String, Object> claims) {
        customClaims.put(uid, Map.copyOf(claims));
        Integer count = claimWrites.get(uid);
        claimWrites.put(uid, count == null ? 1 : count + 1);
    }

    @Override
    public void revokeRefreshTokens(String uid) {
        revokedAt.put(uid, clock.instant());
    }

    private static String newCookie() {
        return "sess-" + UUID.randomUUID();
    }

    private record IssuedSession(VerifiedIdentity identity, Instant issuedAt) {
    }
}
