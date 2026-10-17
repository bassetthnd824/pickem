package com.curleesoft.pickem.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoopbackAddressesTests {

    @Test
    void recognizesOnlyThisHost() {
        assertThat(LoopbackAddresses.isLoopback("127.0.0.1")).isTrue();
        assertThat(LoopbackAddresses.isLoopback("::1")).isTrue();
        assertThat(LoopbackAddresses.isLoopback("https://example.net/id/garnet")).isTrue();
        assertThat(LoopbackAddresses.isLoopback("203.0.113.10")).isFalse();
        assertThat(LoopbackAddresses.isLoopback(null)).isFalse();
        assertThat(InternalJobFilter.isInternal("/api/internal/jobs/cfbd-scores")).isTrue();
        assertThat(InternalJobFilter.isInternal("/api/internal")).isTrue();
        assertThat(InternalJobFilter.isInternal("/api/game/main")).isFalse();
        assertThat(InternalJobFilter.isInternal(null)).isFalse();
    }
}
