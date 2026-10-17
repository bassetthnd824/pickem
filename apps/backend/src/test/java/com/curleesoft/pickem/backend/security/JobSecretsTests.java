package com.curleesoft.pickem.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JobSecretsTests {

    @Test
    void matchesTheSameSecretAndRejectsABlankConfiguredSecret() {
        assertThat(JobSecrets.matches("local-secret", "local-secret")).isTrue();
        assertThat(JobSecrets.matches("local-secret", "other-secret")).isFalse();
        assertThat(JobSecrets.matches("local-secret", "")).isFalse();
        assertThat(JobSecrets.matches("local-secret", "   ")).isFalse();
        assertThat(JobSecrets.matches(null, "local-secret")).isFalse();
    }
}
