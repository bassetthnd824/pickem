package com.curleesoft.pickem.backend.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SeedNamesTests {

    @Test
    void splitsDisplayNameOnTheFirstSpace() {
        SeedNames.Name name = SeedNames.split("Kenney Curlee", "kenney@example.com");

        assertThat(name.firstName()).isEqualTo("Kenney");
        assertThat(name.lastName()).isEqualTo("Curlee");
    }

    @Test
    void usesTheEmailLocalPartWhenTheDisplayNameIsMissing() {
        SeedNames.Name name = SeedNames.split("  ", "boss.manager@example.com");

        assertThat(name.firstName()).isEqualTo("boss.manager");
        assertThat(name.lastName()).isEqualTo(SeedNames.FALLBACK_LAST_NAME);
    }

    @Test
    void singleTokenDisplayNameKeepsAFallbackLastName() {
        SeedNames.Name name = SeedNames.split("Kenney", "kenney@example.com");

        assertThat(name.firstName()).isEqualTo("Kenney");
        assertThat(name.lastName()).isEqualTo(SeedNames.FALLBACK_LAST_NAME);
    }

    @Test
    void truncatesLongNames() {
        String longName = "A".repeat(50) + " " + "B".repeat(50);
        SeedNames.Name name = SeedNames.split(longName, "a@example.com");

        assertThat(name.firstName()).hasSize(40);
        assertThat(name.lastName()).hasSize(40);
    }
}
