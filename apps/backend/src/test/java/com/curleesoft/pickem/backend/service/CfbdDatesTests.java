package com.curleesoft.pickem.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CfbdDatesTests {

    @Test
    void calendarKeepsTheDatePrefixAndKickoffUsesEasternTime() {
        assertThat(CfbdDates.calendarDate("2026-08-31T00:00:00.000Z")).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(CfbdDates.calendarDate("2026-09-07T23:59:59.000Z")).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(CfbdDates.kickoffDate("2026-09-06T03:30:00.000Z")).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(CfbdDates.kickoffDate("2026-09-05")).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(CfbdDates.weekBegin("2024-08-24T00:00:00.000Z")).isEqualTo(LocalDate.of(2024, 8, 23));
        assertThat(CfbdDates.kickoffDate("2024-08-24T02:00:00.000Z")).isEqualTo(LocalDate.of(2024, 8, 23));
        assertThat(CfbdDates.weekEnd("2024-09-02T23:59:59.000Z")).isEqualTo(LocalDate.of(2024, 9, 2));
    }
}
