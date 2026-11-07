package com.curleesoft.pickem.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.google.firebase.auth.SessionCookieOptions;

class FirebaseAdminIdentityClientTests {

    @Test
    void fourteenDaySessionFitsTheAdminSdk() {
        long adjusted = FirebaseAdminIdentityClient.expiresInMillis(Duration.ofDays(14));

        assertThat(adjusted).isEqualTo(TimeUnit.DAYS.toMillis(14) - 1);
        assertThatCode(() -> SessionCookieOptions.builder().setExpiresIn(adjusted).build())
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> SessionCookieOptions.builder().setExpiresIn(Duration.ofDays(14).toMillis()).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fiveMinuteSessionFitsTheAdminSdk() {
        long adjusted = FirebaseAdminIdentityClient.expiresInMillis(Duration.ofMinutes(5));

        assertThat(adjusted).isEqualTo(TimeUnit.MINUTES.toMillis(5) + 1);
        assertThatCode(() -> SessionCookieOptions.builder().setExpiresIn(adjusted).build())
                .doesNotThrowAnyException();
    }

    @Test
    void interiorDurationIsUnchanged() {
        Duration week = Duration.ofDays(7);

        assertThat(FirebaseAdminIdentityClient.expiresInMillis(week)).isEqualTo(week.toMillis());
    }
}
