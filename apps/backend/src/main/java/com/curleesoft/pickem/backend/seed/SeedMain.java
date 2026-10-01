package com.curleesoft.pickem.backend.seed;

import java.time.Clock;
import java.util.Optional;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

/**
 * Entry point for {@code tools/seed}. Uses the Firestore emulator when
 * {@code FIRESTORE_EMULATOR_HOST} is set, and application default credentials
 * otherwise.
 */
public final class SeedMain {

    private SeedMain() {
    }

    public static void main(String[] args) {
        Firestore firestore = null;

        try {
            SeedEnvironment environment = SeedEnvironment.load(args);
            firestore = open(environment);
            FirestoreSeeder seeder = new FirestoreSeeder(firestore, Clock.systemUTC(),
                    new FirebaseBootstrapIdentity(environment));
            Optional<String> manager = seeder.seed(environment.bootstrapEmail());
            report(environment, manager);

        } catch (SeedFailedException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);

        } finally {
            close(firestore);
        }
    }

    private static Firestore open(SeedEnvironment environment) {
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder().setProjectId(environment.projectId());
        String emulatorHost = environment.firestoreEmulatorHost();

        if (emulatorHost != null) {
            builder.setEmulatorHost(emulatorHost).setCredentials(new FirestoreOptions.EmulatorCredentials());
        }

        return builder.build().getService();
    }

    private static void report(SeedEnvironment environment, Optional<String> managerUid) {
        if (environment.firestoreEmulatorHost() != null) {
            System.out.println(
                    "Firestore emulator " + environment.firestoreEmulatorHost() + " project " + environment.projectId());
        } else {
            System.out.println("Firestore project " + environment.projectId() + " (application default credentials)");
        }

        System.out.println("Seeded " + SeedCatalog.venues().size() + " venues, " + SeedCatalog.teams().size()
                + " teams, " + SeedCatalog.rivalries().size() + " rivalries, 1 season, " + SeedCatalog.WEEK_COUNT
                + " weeks, " + SeedCatalog.matchups().size() + " matchups, " + SeedCatalog.themes().size()
                + " themes.");

        if (managerUid.isEmpty()) {
            System.out.println("Bootstrap manager skipped (PICKEM_BOOTSTRAP_MANAGER_EMAIL is unset).");
            return;
        }

        System.out.println("Granted manager and player to " + managerUid.get() + ".");
    }

    private static void close(Firestore firestore) {
        if (firestore == null) {
            return;
        }

        try {
            firestore.close();

        } catch (Exception ex) {
            System.err.println("Failed to close Firestore: " + ex.getMessage());
        }
    }
}
