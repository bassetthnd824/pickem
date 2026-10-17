package com.curleesoft.pickem.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Compares the internal job secret without short-circuiting on the first
 * differing character. A blank configured secret never matches.
 */
public final class JobSecrets {

    private JobSecrets() {
    }

    public static boolean matches(String provided, String expected) {
        if (expected == null || expected.isBlank() || provided == null) {
            return false;
        }
        byte[] actual = digest(provided);
        byte[] required = digest(expected);
        return MessageDigest.isEqual(actual, required);
    }

    private static byte[] digest(String value) {
        String text = Objects.requireNonNull(value, "value");
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
