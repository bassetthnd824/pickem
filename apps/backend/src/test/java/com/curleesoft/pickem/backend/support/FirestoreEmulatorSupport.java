package com.curleesoft.pickem.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.gcloud.FirestoreEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Points Spring tests at a Firestore emulator. Honors
 * {@code FIRESTORE_EMULATOR_HOST} when already set (local firebase / JAR
 * emulator); otherwise starts a Testcontainers emulator for CI.
 */
public abstract class FirestoreEmulatorSupport {

    private static final DockerImageName EMULATOR_IMAGE = DockerImageName
            .parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators");

    private static final String EXTERNAL_HOST = firstNonBlank(System.getenv("FIRESTORE_EMULATOR_HOST"),
            System.getProperty("FIRESTORE_EMULATOR_HOST"));

    private static final FirestoreEmulatorContainer CONTAINER = startIfNeeded();

    private static FirestoreEmulatorContainer startIfNeeded() {
        if (EXTERNAL_HOST != null) {
            return null;
        }
        FirestoreEmulatorContainer container = new FirestoreEmulatorContainer(EMULATOR_IMAGE);
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void registerFirestoreProperties(DynamicPropertyRegistry registry) {
        String host = emulatorEndpoint();
        registry.add("pickem.firestore.emulator-enabled", () -> "true");
        registry.add("pickem.firestore.emulator-host", () -> host);
        registry.add("pickem.firestore.project-id", () -> "pickem-test");
    }

    protected static String emulatorEndpoint() {
        if (EXTERNAL_HOST != null) {
            return EXTERNAL_HOST;
        }
        if (CONTAINER == null) {
            throw new IllegalStateException("Firestore emulator container was not started");
        }
        return CONTAINER.getEmulatorEndpoint();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
