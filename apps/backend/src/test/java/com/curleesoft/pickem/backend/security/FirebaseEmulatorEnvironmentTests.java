package com.curleesoft.pickem.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.firebase.internal.FirebaseProcessEnvironment;

class FirebaseEmulatorEnvironmentTests {

    @AfterEach
    void clearOverride() {
        FirebaseProcessEnvironment.clearCache();
    }

    @Test
    void publishesDotenvHostWhenTheProcessEnvironmentIsUnset() {
        String fromOs = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
        if (fromOs != null && !fromOs.isBlank()) {
            FirebaseEmulatorEnvironment.publish("127.0.0.1:1");
            assertThat(FirebaseProcessEnvironment.getenv("FIREBASE_AUTH_EMULATOR_HOST")).isEqualTo(fromOs);
            return;
        }

        FirebaseEmulatorEnvironment.publish("127.0.0.1:9099");

        assertThat(FirebaseProcessEnvironment.getenv("FIREBASE_AUTH_EMULATOR_HOST")).isEqualTo("127.0.0.1:9099");
    }

    @Test
    void ignoresABlankHost() {
        FirebaseEmulatorEnvironment.publish("  ");
        String fromOs = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
        assertThat(FirebaseProcessEnvironment.getenv("FIREBASE_AUTH_EMULATOR_HOST")).isEqualTo(fromOs);
    }
}
