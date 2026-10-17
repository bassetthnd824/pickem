package com.curleesoft.pickem.backend.seed;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.curleesoft.pickem.backend.security.RoleClaims;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.internal.FirebaseProcessEnvironment;

/**
 * Firebase Admin lookup used by {@link SeedMain}. Initialized on the first
 * call so a reference-data seed does not need credentials when no bootstrap
 * email is set.
 */
public final class FirebaseBootstrapIdentity implements BootstrapIdentity {

    private static final String APP_NAME = "pickem-seed";

    private final SeedEnvironment environment;

    private volatile FirebaseAuth firebaseAuth;

    public FirebaseBootstrapIdentity(SeedEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Optional<BootstrapAccount> findByEmail(String email) {
        try {
            UserRecord user = auth().getUserByEmail(email);
            return Optional.of(toAccount(user));

        } catch (FirebaseAuthException ex) {
            if (ex.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                return Optional.empty();
            }

            throw new SeedFailedException("Could not look up Firebase user " + email, ex);
        }
    }

    @Override
    public void grantManagerAndPlayer(String uid) {
        try {
            auth().setCustomUserClaims(uid, RoleClaims.forRoles(List.of(RoleClaims.PLAYER, RoleClaims.MANAGER)));

        } catch (FirebaseAuthException ex) {
            throw new SeedFailedException("Could not grant manager and player claims to " + uid, ex);
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
        publishAuthEmulator();
        boolean emulator = authEmulatorConfigured();

        try {
            FirebaseOptions.Builder builder = FirebaseOptions.builder().setProjectId(environment.projectId());

            if (emulator) {
                builder.setCredentials(GoogleCredentials
                        .create(new AccessToken("owner", Date.from(Instant.now().plus(Duration.ofHours(1))))));
            } else {
                builder.setCredentials(GoogleCredentials.getApplicationDefault());
            }

            return FirebaseAuth.getInstance(firebaseApp(builder.build()));

        } catch (IOException ex) {
            throw new SeedFailedException("Firebase Admin credentials are not configured", ex);
        }
    }

    private void publishAuthEmulator() {
        String host = environment.authEmulatorHost();

        if (host == null || host.isBlank()) {
            return;
        }

        String current = System.getenv(SeedEnvironment.AUTH_HOST);

        if (current != null && !current.isBlank()) {
            return;
        }

        FirebaseProcessEnvironment.setenv(SeedEnvironment.AUTH_HOST, host);
    }

    private boolean authEmulatorConfigured() {
        String fromEnv = System.getenv(SeedEnvironment.AUTH_HOST);

        if (fromEnv != null && !fromEnv.isBlank()) {
            return true;
        }

        String configured = environment.authEmulatorHost();
        return configured != null && !configured.isBlank();
    }

    private static FirebaseApp firebaseApp(FirebaseOptions options) {
        synchronized (FirebaseApp.class) {
            for (FirebaseApp app : FirebaseApp.getApps()) {
                if (APP_NAME.equals(app.getName())) {
                    return app;
                }
            }

            return FirebaseApp.initializeApp(options, APP_NAME);
        }
    }

    private static BootstrapAccount toAccount(UserRecord user) {
        String email = user.getEmail() == null ? "" : user.getEmail();
        SeedNames.Name name = SeedNames.split(user.getDisplayName(), email);
        return new BootstrapAccount(user.getUid(), email, name.firstName(), name.lastName());
    }
}
