package com.curleesoft.pickem.backend.security;

/**
 * Email and name taken from a verified Google sign-in.
 */
public record GoogleProfile(String email, String firstName, String lastName) {
    public static GoogleProfile from(VerifiedIdentity identity) {
        if (identity == null || !"google.com".equals(identity.signInProvider())) {
            throw new InvalidCredentialException("Google sign-in is required");
        }
        String email = identity.email();
        if (email == null || email.isBlank() || email.indexOf('@') < 1) {
            throw new InvalidCredentialException("Google profile has no email");
        }
        if (hasText(identity.givenName()) && hasText(identity.familyName())) {
            return new GoogleProfile(email.trim(), identity.givenName().trim(), identity.familyName().trim());
        }
        String displayName = identity.displayName();
        if (!hasText(displayName)) {
            throw new InvalidCredentialException("Google profile has no name");
        }
        String[] parts = displayName.trim().split("\\s+", 2);
        String firstName = parts[0];
        // A one-word Google display name fills lastName so the required field
        // is present. given_name and family_name win when Google sends both.
        String lastName = parts.length > 1 ? parts[1] : firstName;
        return new GoogleProfile(email.trim(), firstName, lastName);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
