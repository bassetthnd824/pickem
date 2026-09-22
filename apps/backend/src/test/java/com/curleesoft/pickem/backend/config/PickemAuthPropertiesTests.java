package com.curleesoft.pickem.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class PickemAuthPropertiesTests {

    @Test
    void acceptsTheFiveToFourteenDayWindow() {
        PickemAuthProperties shortest = new PickemAuthProperties("firebase", Duration.ofMinutes(5), "");
        PickemAuthProperties longest = new PickemAuthProperties("firebase", Duration.ofDays(14), "");

        assertThat(shortest.sessionDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(longest.sessionDuration()).isEqualTo(Duration.ofDays(14));
    }

    @Test
    void rejectsDurationsOutsideTheFirebaseWindow() {
        assertThatThrownBy(() -> new PickemAuthProperties("firebase", Duration.ofMinutes(4), ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PickemAuthProperties("firebase", Duration.ofDays(14).plusSeconds(1), ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
