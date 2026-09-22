package com.curleesoft.pickem.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.curleesoft.pickem.backend.model.Season;

class SeasonCalendarTests {

    @Test
    void validationMessagesMatchTheLegacyWording() {
        assertThat(SeasonCalendar.BEGIN_DATE_INVALID).isEqualTo("season begin date is invalid");
        assertThat(SeasonCalendar.END_DATE_INVALID).isEqualTo("season end date is invalid");
        assertThat(SeasonCalendar.WEEK_BEGIN_DATE_INVALID).isEqualTo("season week begin date is invalid");
        assertThat(SeasonCalendar.WEEK_END_DATE_INVALID).isEqualTo("season week end date is invalid");
        assertThat(SeasonCalendar.WEEK_BEGIN_ALIGNMENT_INVALID).isEqualTo("Week Begin Date is invalid");
    }

    @Test
    void seasonBeginYearMatchesAndEndYearIsTheSeasonYearOrTheNext() {
        SeasonCalendar.ParsedSeason sameYear = SeasonCalendar.parseSeason("2024", "2024-08-29", "2024-12-31");
        assertThat(sameYear.beginDate()).isEqualTo(LocalDate.parse("2024-08-29"));
        assertThat(sameYear.endDate()).isEqualTo(LocalDate.parse("2024-12-31"));

        SeasonCalendar.ParsedSeason nextYear = SeasonCalendar.parseSeason("2024", "2024-08-29", "2025-01-20");
        assertThat(nextYear.endDate()).isEqualTo(LocalDate.parse("2025-01-20"));

        assertThatThrownBy(() -> SeasonCalendar.parseSeason("2024", "2025-08-29", "2026-01-11"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.BEGIN_DATE_INVALID);
        assertThatThrownBy(() -> SeasonCalendar.parseSeason("2024", "2024-08-29", "2026-01-11"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.END_DATE_INVALID);
        assertThatThrownBy(() -> SeasonCalendar.parseSeason("2024", "2024-12-01", "2024-08-01"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.END_DATE_INVALID);
        assertThatThrownBy(() -> SeasonCalendar.parseSeason("24", "2024-08-29", "2025-01-20"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.BEGIN_DATE_INVALID);
        assertThatThrownBy(() -> SeasonCalendar.parseSeason("abcd", "2024-08-29", "2025-01-20"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.BEGIN_DATE_INVALID);
    }

    @Test
    void manualWeeksStartThursdayAndFollowTheSeasonGrid() {
        LocalDate weekOne = LocalDate.parse("2024-08-29");
        assertThat(weekOne.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);

        SeasonCalendar.ParsedWeek first = SeasonCalendar.parseManualWeek("2024-08-29", 1, "2024-08-29", "2024-09-04");
        assertThat(first.beginDate()).isEqualTo(weekOne);
        assertThat(first.endDate()).isEqualTo(weekOne.plusDays(6));
        assertThat(first.endDate().getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);

        SeasonCalendar.ParsedWeek second = SeasonCalendar.parseManualWeek("2024-08-29", 2, "2024-09-05", "2024-09-11");
        assertThat(second.beginDate()).isEqualTo(weekOne.plusDays(7));

        assertThatThrownBy(() -> SeasonCalendar.parseManualWeek("2024-08-29", 1, "2024-08-30", "2024-09-05"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.WEEK_BEGIN_DATE_INVALID);
        assertThatThrownBy(() -> SeasonCalendar.parseManualWeek("2024-08-29", 1, "2024-08-29", "2024-09-03"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.WEEK_END_DATE_INVALID);
        assertThatThrownBy(() -> SeasonCalendar.parseManualWeek("2024-08-29", 2, "2024-08-29", "2024-09-04"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.WEEK_BEGIN_ALIGNMENT_INVALID);
        assertThatThrownBy(() -> SeasonCalendar.parseManualWeek("2024-08-29", 0, "2024-08-22", "2024-08-28"))
                .isInstanceOf(InvalidRequestException.class).hasMessage(SeasonCalendar.WEEK_NUMBER_INVALID);
    }

    @Test
    void currentWindowIsBeginMinusFiveMonthsThroughEndPlusOneMonth() {
        LocalDate begin = LocalDate.parse("2024-08-29");
        LocalDate end = LocalDate.parse("2025-01-13");

        assertThat(SeasonCalendar.inCurrentWindow(begin, end, LocalDate.parse("2024-03-29"))).isTrue();
        assertThat(SeasonCalendar.inCurrentWindow(begin, end, LocalDate.parse("2024-03-28"))).isFalse();
        assertThat(SeasonCalendar.inCurrentWindow(begin, end, LocalDate.parse("2025-02-13"))).isTrue();
        assertThat(SeasonCalendar.inCurrentWindow(begin, end, LocalDate.parse("2025-02-14"))).isFalse();
    }

    @Test
    void currentSeasonPrefersTheFlagThenTheDateWindow() {
        Season flagged = season("2020", "2020-08-27", "2021-01-10", true);
        Season inWindow = season("2026", "2026-08-27", "2027-01-10", false);

        assertThat(SeasonCalendar.selectCurrent(List.of(inWindow, flagged), LocalDate.parse("2026-10-01")))
                .map(Season::getSeason).contains("2020");

        Season older = season("2024", "2024-01-15", "2024-12-15", false);
        Season newer = season("2025", "2025-01-15", "2025-12-15", false);
        assertThat(SeasonCalendar.selectCurrent(List.of(older, newer), LocalDate.parse("2025-01-01")))
                .map(Season::getSeason).contains("2025");

        Season flaggedLater = season("2026", "2026-08-27", "2027-01-10", true);
        Season flaggedInWindow = season("2024", "2024-08-29", "2025-01-13", true);
        assertThat(SeasonCalendar.selectCurrent(List.of(flaggedLater, flaggedInWindow), LocalDate.parse("2024-10-01")))
                .map(Season::getSeason).contains("2024");

        Season twenty = season("2020", "2020-01-15", "2020-12-15", true);
        Season twentyOne = season("2021", "2021-01-15", "2021-12-15", true);
        assertThat(SeasonCalendar.selectCurrent(List.of(twenty, twentyOne), LocalDate.parse("2026-09-21")))
                .map(Season::getSeason).contains("2021");

        assertThat(SeasonCalendar.selectCurrent(List.of(), LocalDate.parse("2026-09-21"))).isEmpty();
    }

    private static Season season(String year, String begin, String end, boolean current) {
        Season season = new Season();
        season.setSeason(year);
        season.setBeginDate(begin);
        season.setEndDate(end);
        season.setCurrent(current);
        return season;
    }
}
