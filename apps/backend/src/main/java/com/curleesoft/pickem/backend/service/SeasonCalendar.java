package com.curleesoft.pickem.backend.service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.curleesoft.pickem.backend.model.Season;

/**
 * Calendar rules for seasons and manually entered weeks. Ports
 * {@code ValidSeasonBeginDate}, {@code ValidSeasonEndDate},
 * {@code ValidSeasonWeekBeginDate}, {@code ValidSeasonWeekEndDate},
 * {@code SeasonWeekAction}, and the Derby {@code getCurrentSeason} window.
 */
public final class SeasonCalendar {

    public static final String BEGIN_DATE_INVALID = "season begin date is invalid";

    public static final String END_DATE_INVALID = "season end date is invalid";

    public static final String WEEK_BEGIN_DATE_INVALID = "season week begin date is invalid";

    public static final String WEEK_END_DATE_INVALID = "season week end date is invalid";

    public static final String WEEK_BEGIN_ALIGNMENT_INVALID = "Week Begin Date is invalid";

    public static final String WEEK_NUMBER_INVALID = "week number is invalid";

    private SeasonCalendar() {
    }

    public static ParsedSeason parseSeason(String season, String beginDate, String endDate) {
        int year = seasonYear(season);
        LocalDate begin = parseDate(beginDate, BEGIN_DATE_INVALID);

        if (begin.getYear() != year) {
            throw new InvalidRequestException(BEGIN_DATE_INVALID);
        }

        LocalDate end = parseDate(endDate, END_DATE_INVALID);

        if (end.getYear() != year && end.getYear() != year + 1) {
            throw new InvalidRequestException(END_DATE_INVALID);
        }

        if (end.isBefore(begin)) {
            throw new InvalidRequestException(END_DATE_INVALID);
        }

        return new ParsedSeason(season, begin, end);
    }

    /**
     * Manual week CRUD only. Week 1 begins on the season begin date; later weeks
     * begin {@code (weekNumber - 1) * 7} days later. Begin is Thursday and end is
     * the following Wednesday.
     */
    public static ParsedWeek parseManualWeek(String seasonBeginDate, int weekNumber, String beginDate, String endDate) {
        if (weekNumber < 1) {
            throw new InvalidRequestException(WEEK_NUMBER_INVALID);
        }

        LocalDate seasonBegin = parseDate(seasonBeginDate, WEEK_BEGIN_ALIGNMENT_INVALID);
        LocalDate begin = parseDate(beginDate, WEEK_BEGIN_DATE_INVALID);

        if (begin.getDayOfWeek() != DayOfWeek.THURSDAY) {
            throw new InvalidRequestException(WEEK_BEGIN_DATE_INVALID);
        }

        LocalDate end = parseDate(endDate, WEEK_END_DATE_INVALID);

        if (end.getDayOfWeek() != DayOfWeek.WEDNESDAY || !end.equals(begin.plusDays(6))) {
            throw new InvalidRequestException(WEEK_END_DATE_INVALID);
        }

        if (!begin.equals(expectedBegin(seasonBegin, weekNumber))) {
            throw new InvalidRequestException(WEEK_BEGIN_ALIGNMENT_INVALID);
        }

        return new ParsedWeek(begin, end);
    }

    /**
     * A season marked {@code isCurrent} wins. Otherwise the season whose Derby
     * window contains {@code today} wins: begin minus 5 months through end plus 1
     * month, inclusive. Several matches resolve to the latest season year.
     */
    public static Optional<Season> selectCurrent(List<Season> seasons, LocalDate today) {
        if (seasons == null || seasons.isEmpty() || today == null) {
            return Optional.empty();
        }

        List<Season> flagged = seasons.stream().filter(Season::isCurrent).toList();

        if (!flagged.isEmpty()) {
            return Optional.of(preferFlagged(flagged, today));
        }

        return latest(seasons.stream().filter(season -> inCurrentWindow(season, today)).toList());
    }

    public static boolean inCurrentWindow(LocalDate begin, LocalDate end, LocalDate today) {
        LocalDate windowStart = begin.minusMonths(5);
        LocalDate windowEnd = end.plusMonths(1);
        return !today.isBefore(windowStart) && !today.isAfter(windowEnd);
    }

    private static Season preferFlagged(List<Season> flagged, LocalDate today) {
        List<Season> inWindow = flagged.stream().filter(season -> inCurrentWindow(season, today)).toList();

        if (inWindow.size() == 1) {
            return inWindow.get(0);
        }

        if (!inWindow.isEmpty()) {
            return latest(inWindow).orElseThrow();
        }

        return latest(flagged).orElseThrow();
    }

    private static boolean inCurrentWindow(Season season, LocalDate today) {
        if (season.getBeginDate() == null || season.getEndDate() == null) {
            return false;
        }

        try {
            return inCurrentWindow(LocalDate.parse(season.getBeginDate()), LocalDate.parse(season.getEndDate()), today);
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static Optional<Season> latest(List<Season> seasons) {
        return seasons.stream().max(Comparator.comparing(Season::getSeason, Comparator.nullsLast(String::compareTo)));
    }

    private static LocalDate expectedBegin(LocalDate seasonBegin, int weekNumber) {
        try {
            return seasonBegin.plusDays((weekNumber - 1L) * 7L);
        } catch (DateTimeException ex) {
            throw new InvalidRequestException(WEEK_BEGIN_ALIGNMENT_INVALID);
        }
    }

    private static int seasonYear(String season) {
        if (season == null || !season.matches("\\d{4}")) {
            throw new InvalidRequestException(BEGIN_DATE_INVALID);
        }

        return Integer.parseInt(season);
    }

    private static LocalDate parseDate(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(message);
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestException(message);
        }
    }

    public record ParsedSeason(String season, LocalDate beginDate, LocalDate endDate) {
    }

    public record ParsedWeek(LocalDate beginDate, LocalDate endDate) {
    }
}
