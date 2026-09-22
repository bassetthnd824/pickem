package com.curleesoft.pickem.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class GoogleProfileTests {

    @Test
    void prefersGivenAndFamilyNames() {
        GoogleProfile profile = GoogleProfile.from(identity("Kenney Curlee", "Kenney", "Curlee", "google.com"));

        assertThat(profile.email()).isEqualTo("kenney@example.com");
        assertThat(profile.firstName()).isEqualTo("Kenney");
        assertThat(profile.lastName()).isEqualTo("Curlee");
    }

    @Test
    void splitsDisplayNameWhenGoogleOmitsGivenAndFamily() {
        GoogleProfile profile = GoogleProfile.from(identity("Kenney Curlee", "", "", "google.com"));

        assertThat(profile.firstName()).isEqualTo("Kenney");
        assertThat(profile.lastName()).isEqualTo("Curlee");
    }

    @Test
    void singleWordDisplayNameFillsLastName() {
        GoogleProfile profile = GoogleProfile.from(identity("Kenney", "", "", "google.com"));

        assertThat(profile.firstName()).isEqualTo("Kenney");
        assertThat(profile.lastName()).isEqualTo("Kenney");
    }

    @Test
    void rejectsNonGoogleProviders() {
        assertThatThrownBy(() -> GoogleProfile.from(identity("Kenney Curlee", "Kenney", "Curlee", "password")))
                .isInstanceOf(InvalidCredentialException.class).hasMessageContaining("Google");
    }

    @Test
    void rejectsMissingEmailOrName() {
        assertThatThrownBy(() -> GoogleProfile
                .from(new VerifiedIdentity("uid", " ", "Kenney", "Kenney", "C", "google.com", Map.of())))
                        .isInstanceOf(InvalidCredentialException.class);

        assertThatThrownBy(() -> GoogleProfile.from(identity(" ", "", "", "google.com")))
                .isInstanceOf(InvalidCredentialException.class);
    }

    private static VerifiedIdentity identity(String displayName, String givenName, String familyName, String provider) {
        return new VerifiedIdentity("uid", "kenney@example.com", displayName, givenName, familyName, provider,
                Map.of());
    }
}
