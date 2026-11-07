package com.curleesoft.pickem.backend.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Ports {@code MatchupUserPick.getScore} and {@code isCurrentOrPastWeek}. A
 * correct pick earns its rank only when both scores exist, they are not equal,
 * and the pick is the winner. Equal scores score 0 even if a winner id is
 * stored. Week currency uses the league calendar date in {@link #LEAGUE_ZONE},
 * not the JVM zone.
 */
public final class PickScoring {

    public static final ZoneId LEAGUE_ZONE = ZoneId.of("America/New_York");

    private PickScoring() {
    }

    public static boolean currentOrPast(String weekBeginDate, Clock clock) {
        if (weekBeginDate == null || weekBeginDate.isBlank() || clock == null) {
            return false;
        }

        LocalDate begin;

        try {
            begin = LocalDate.parse(weekBeginDate);
        } catch (DateTimeParseException ex) {
            return false;
        }

        LocalDate today;

        try {
            today = LocalDate.ofInstant(clock.instant(), LEAGUE_ZONE);
        } catch (DateTimeException ex) {
            return false;
        }

        return !today.isBefore(begin);
    }

    public static int points(Integer homeScore, Integer awayScore, String winningTeamId, String pickedTeamId,
            Integer rank) {
        if (homeScore == null || awayScore == null || rank == null || homeScore.equals(awayScore)) {
            return 0;
        }

        if (pickedTeamId != null && pickedTeamId.equals(winningTeamId)) {
            return rank;
        }

        return 0;
    }
}