package com.curleesoft.pickem.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PickemInternalPropertiesTests {

    @Test
    void trimsTheSecretAndKeepsLoopbackOffCloudRun() {
        PickemInternalProperties properties = new PickemInternalProperties("  local-secret  ", true);

        assertThat(properties.jobSecret()).isEqualTo("local-secret");
        assertThat(properties.allowsLoopback(null)).isTrue();
        assertThat(properties.allowsLoopback("  ")).isTrue();
        assertThat(properties.allowsLoopback("backend")).isFalse();
        assertThat(new PickemInternalProperties("secret", false).allowsLoopback(null)).isFalse();
    }
}
