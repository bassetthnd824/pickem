package com.curleesoft.pickem.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * CFBD timestamps. Kickoffs use {@code America/New_York}. A stored week bound
 * also includes that Eastern date when it is outside the labeled calendar day,
 * so a kickoff in the first hours after a midnight-UTC week start still falls
 * inside the week.
 */
final class CfbdDates {

    static final ZoneId EASTERN = ZoneId.of("America/New_York");

    private CfbdDates() {
    }

    static LocalDate calendarDate(String timestamp) {
        String value = required(timestamp);
        if (value.length() >= 10) {
            try {
                return LocalDate.parse(value.substring(0, 10));
            } catch (DateTimeParseException ex) {
                return LocalDate.ofInstant(instant(value), EASTERN);
            }
        }
        return LocalDate.ofInstant(instant(value), EASTERN);
    }

    static LocalDate weekBegin(String timestamp) {
        return earlier(calendarDate(timestamp), kickoffDate(timestamp));
    }

    static LocalDate weekEnd(String timestamp) {
        return later(calendarDate(timestamp), kickoffDate(timestamp));
    }

    static LocalDate kickoffDate(String timestamp) {
        String value = required(timestamp);
        if (value.length() == 10) {
            return LocalDate.parse(value);
        }
        return LocalDate.ofInstant(instant(value), EASTERN);
    }

    private static String required(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            throw new InvalidRequestException("CFBD date is invalid");
        }
        return timestamp.trim();
    }

    private static LocalDate earlier(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private static LocalDate later(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private static Instant instant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestException("CFBD date is invalid");
        }
    }
}
