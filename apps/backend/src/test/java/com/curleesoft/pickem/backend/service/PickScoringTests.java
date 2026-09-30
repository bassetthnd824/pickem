package com.curleesoft.pickem.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class PickScoringTests {

    @Test
    void pointsFollowGetScore() {
        assertThat(PickScoring.points(31, 14, "home", "home", 5)).isEqualTo(5);
        assertThat(PickScoring.points(10, 24, "away", "home", 5)).isZero();
        assertThat(PickScoring.points(null, null, null, "home", 5)).isZero();
        assertThat(PickScoring.points(21, null, null, "home", 5)).isZero();
        assertThat(PickScoring.points(14, 14, null, "away", 5)).isZero();
        assertThat(PickScoring.points(31, 14, "home", null, 5)).isZero();
        assertThat(PickScoring.points(31, 14, "home", "home", null)).isZero();
    }

    @Test
    void currentOrPastUsesTheLeagueDate() {
        // 03:30 UTC is still the previous evening in America/New_York.
        Clock beforeMidnight = Clock.fixed(Instant.parse("2026-09-24T03:30:00Z"), ZoneOffset.UTC);
        assertThat(PickScoring.currentOrPast("2026-09-24", beforeMidnight)).isFalse();
        assertThat(PickScoring.currentOrPast("2026-09-23", beforeMidnight)).isTrue();

        Clock afternoon = Clock.fixed(Instant.parse("2026-09-24T16:00:00Z"), ZoneOffset.UTC);
        assertThat(PickScoring.currentOrPast("2026-09-24", afternoon)).isTrue();
        assertThat(PickScoring.currentOrPast("2026-10-01", afternoon)).isFalse();
        assertThat(PickScoring.currentOrPast("not-a-date", afternoon)).isFalse();
        assertThat(PickScoring.currentOrPast(null, afternoon)).isFalse();
    }
}