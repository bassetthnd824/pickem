package com.curleesoft.pickem.backend.seed;

/**
 * Splits a Firebase display name into the first and last name stored on
 * {@code users}. A missing display name uses the email local part.
 */
final class SeedNames {

    static final String FALLBACK_LAST_NAME = "Manager";

    private static final int MAX_LENGTH = 40;

    private SeedNames() {
    }

    static Name split(String displayName, String email) {
        String local = localPart(email);

        if (displayName == null || displayName.isBlank()) {
            return new Name(truncate(local), FALLBACK_LAST_NAME);
        }

        String trimmed = displayName.trim();
        int space = trimmed.indexOf(' ');

        if (space < 0) {
            String only = truncate(trimmed);
            return new Name(only.isBlank() ? truncate(local) : only, FALLBACK_LAST_NAME);
        }

        String first = truncate(trimmed.substring(0, space).trim());
        String rest = trimmed.substring(space + 1).trim();

        if (first.isBlank()) {
            first = truncate(local);
        }

        String last = rest.isBlank() ? FALLBACK_LAST_NAME : truncate(rest);
        return new Name(first, last);
    }

    private static String localPart(String email) {
        if (email == null || email.isBlank()) {
            return FALLBACK_LAST_NAME;
        }

        int at = email.indexOf('@');
        String local = (at < 0 ? email : email.substring(0, at)).trim();
        return local.isBlank() ? FALLBACK_LAST_NAME : local;
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_LENGTH);
    }

    record Name(String firstName, String lastName) {
    }
}
