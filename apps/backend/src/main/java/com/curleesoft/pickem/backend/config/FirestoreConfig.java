package com.curleesoft.pickem.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

/**
 * Builds the Firestore client. Production uses ADC against the real project.
 * Local and tests point at the Firestore emulator and skip GCP credentials.
 */
@Configuration
public class FirestoreConfig {

    @Bean(destroyMethod = "close")
    public Firestore firestore(PickemFirestoreProperties properties) {
        String projectId = properties.projectId();
        if (projectId == null || projectId.isBlank()) {
            projectId = "pickem-local";
        }

        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder().setProjectId(projectId);

        String emulatorHost = resolveEmulatorHost(properties);
        if (emulatorHost != null) {
            builder.setEmulatorHost(emulatorHost).setCredentials(new FirestoreOptions.EmulatorCredentials());
        }

        return builder.build().getService();
    }

    static String resolveEmulatorHost(PickemFirestoreProperties properties) {
        String fromEnv = System.getenv("FIRESTORE_EMULATOR_HOST");
        if (fromEnv == null || fromEnv.isBlank()) {
            fromEnv = System.getProperty("FIRESTORE_EMULATOR_HOST");
        }
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String configuredHost = properties.emulatorHost();
        if (properties.emulatorEnabled() && configuredHost != null && !configuredHost.isBlank()) {
            return configuredHost.trim();
        }
        return null;
    }
}
