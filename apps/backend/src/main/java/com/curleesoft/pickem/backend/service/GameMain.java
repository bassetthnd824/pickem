package com.curleesoft.pickem.backend.service;

import java.util.List;

import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;

/**
 * {@code GET /api/game/main}. Weeks are ordered by week number. Matchups inside
 * a week follow the legacy grid: rank descending (unpicked last), then date,
 * then home team name. Future weeks null out scores, the winner, and points.
 */
public record GameMain(String seasonId, String season, int numberOfConferenceTeams, List<GameWeek> weeks) {

    public record GameWeek(String id, Integer weekNumber, String beginDate, String endDate, boolean currentOrPast,
            List<GameMatchup> matchups) {
    }

    public record GameMatchup(String matchupId, String matchupDate, TeamSquadSnapshot awayTeam, Integer awayTeamScore,
            TeamSquadSnapshot homeTeam, Integer homeTeamScore, VenueSnapshot venue, String rivalryName, String pickId,
            String pickedTeamId, Integer rank, String winningTeamId, Integer points) {
    }
}