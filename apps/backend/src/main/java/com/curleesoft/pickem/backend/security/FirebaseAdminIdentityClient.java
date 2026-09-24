package com.curleesoft.pickem.backend.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.curleesoft.pickem.backend.config.PickemAuthProperties;
import com.curleesoft.pickem.backend.config.PickemFirestoreProperties;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.SessionCookieOptions;

/**
 * Production {@link FirebaseIdentityClient}. Firebase is initialized on the
 * first auth call so the API can boot before credentials are present.
 */
@Component
@ConditionalOnProperty(prefix = "pickem.auth", name = "provider", havingValue = "firebase", matchIfMissing = true)
public class FirebaseAdminIdentityClient implements FirebaseIdentityClient {

    private final PickemAuthProperties authProperties;

    private final PickemFirestoreProperties firestoreProperties;

    private volatile FirebaseAuth firebaseAuth;

    public FirebaseAdminIdentityClient(PickemAuthProperties authProperties,
            PickemFirestoreProperties firestoreProperties) {
        this.authProperties = authProperties;
        this.firestoreProperties = firestoreProperties;
    }

    @Override
    public VerifiedIdentity verifyIdToken(String idToken) {
        try {
            return toIdentity(auth().verifyIdToken(idToken, true));
        } catch (FirebaseAuthException ex) {
            throw new InvalidCredentialException("Invalid Google ID token", ex);
        }
    }

    @Override
    public String createSessionCookie(String idToken, Duration expiresIn) {
        try {
            SessionCookieOptions options = SessionCookieOptions.builder().setExpiresIn(expiresIn.toMillis()).build();
            return auth().createSessionCookie(idToken, options);
        } catch (FirebaseAuthException ex) {
            throw new InvalidCredentialException("Could not create a session cookie", ex);
        }
    }

    @Override
    public VerifiedIdentity verifySessionCookie(String sessionCookie) {
        try {
            return toIdentity(auth().verifySessionCookie(sessionCookie, true));
        } catch (FirebaseAuthException ex) {
            throw new InvalidCredentialException("Invalid or revoked session cookie", ex);
        }
    }

    @Override
    public void setCustomUserClaims(String uid, Map<String, Object> claims) {
        try {
            auth().setCustomUserClaims(uid, claims);
        } catch (FirebaseAuthException ex) {
            throw new InvalidCredentialException("Could not set custom user claims", ex);
        }
    }

    @Override
    public void revokeRefreshTokens(String uid) {
        try {
            auth().revokeRefreshTokens(uid);
        } catch (FirebaseAuthException ex) {
            throw new InvalidCredentialException("Could not revoke the session", ex);
        }
    }

    private FirebaseAuth auth() {
        FirebaseAuth current = firebaseAuth;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (firebaseAuth == null) {
                firebaseAuth = initialize();
            }
            return firebaseAuth;
        }
    }

    private FirebaseAuth initialize() {
        FirebaseEmulatorEnvironment.publish(authProperties.emulatorHost());
        String projectId = firestoreProperties.projectId();
        if (projectId == null || projectId.isBlank()) {
            projectId = "pickem-local";
        }
        boolean emulator = emulatorConfigured();
        try {
            FirebaseOptions.Builder builder = FirebaseOptions.builder().setProjectId(projectId);
            if (emulator) {
                builder.setCredentials(GoogleCredentials
                        .create(new AccessToken("owner", Date.from(Instant.now().plus(Duration.ofHours(1))))));
            } else {
                builder.setCredentials(GoogleCredentials.getApplicationDefault());
            }
            FirebaseApp app = firebaseApp(builder.build());
            return FirebaseAuth.getInstance(app);
        } catch (IOException ex) {
            throw new AuthUnavailableException("Firebase Admin credentials are not configured", ex);
        }
    }

    private boolean emulatorConfigured() {
        String fromEnv = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return true;
        }
        String configured = authProperties.emulatorHost();
        return configured != null && !configured.isBlank();
    }

    private static FirebaseApp firebaseApp(FirebaseOptions options) {
        synchronized (FirebaseApp.class) {
            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }

    private static VerifiedIdentity toIdentity(FirebaseToken token) {
        Map<String, Object> claims = copyClaims(token.getClaims());
        return new VerifiedIdentity(token.getUid(), token.getEmail(), token.getName(),
                stringClaim(claims, "given_name"), stringClaim(claims, "family_name"), signInProvider(claims), claims);
    }

    private static Map<String, Object> copyClaims(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(copy);
    }

    private static String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return "";
    }

    private static String signInProvider(Map<String, Object> claims) {
        Object firebase = claims.get("firebase");
        if (firebase instanceof Map<?, ?> map) {
            Object provider = map.get("sign_in_provider");
            if (provider instanceof String text) {
                return text;
            }
        }
        return "";
    }
}
