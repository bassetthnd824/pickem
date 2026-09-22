package com.curleesoft.pickem.backend.security;

import com.google.firebase.internal.FirebaseProcessEnvironment;

/**
 * The Firebase Admin SDK reads {@code FIREBASE_AUTH_EMULATOR_HOST} from the
 * process environment. Values loaded from {@code .env} into the Spring
 * Environment are published here when the OS variable is unset.
 */
final class FirebaseEmulatorEnvironment {

    private FirebaseEmulatorEnvironment() {
    }

    static void publish(String emulatorHost) {
        if (emulatorHost == null || emulatorHost.isBlank()) {
            return;
        }
        String current = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
        if (current != null && !current.isBlank()) {
            return;
        }
        FirebaseProcessEnvironment.setenv("FIREBASE_AUTH_EMULATOR_HOST", emulatorHost.trim());
    }
}
