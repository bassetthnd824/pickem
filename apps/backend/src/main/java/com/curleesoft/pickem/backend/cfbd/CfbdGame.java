package com.curleesoft.pickem.backend.cfbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row from CFBD {@code GET /games}. {@code homeCfbdTeamId} and
 * {@code awayCfbdTeamId} are CFBD {@code homeId} and {@code awayId}, stored on
 * teams as {@code cfbdTeamId}. {@code cfbdVenueId} is CFBD {@code venueId}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CfbdGame(long id, int season, int week, String seasonType, String startDate, boolean completed,
        String venue, @JsonProperty("venueId") Long cfbdVenueId, @JsonProperty("homeId") long homeCfbdTeamId,
        @JsonProperty("homeTeam") String homeTeam, Integer homePoints, @JsonProperty("awayId") long awayCfbdTeamId,
        @JsonProperty("awayTeam") String awayTeam, Integer awayPoints) {
}
