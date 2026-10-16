package com.curleesoft.pickem.backend.cfbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row from CFBD {@code GET /scoreboard}. {@code status} is the current-day
 * game state ({@code scheduled}, {@code in_progress}, or {@code completed}).
 * Team {@code id} values are {@code cfbdTeamId}. The scoreboard venue has no
 * CFBD venue id. Patreon weather and betting fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CfbdScoreboardGame(long id, String startDate, String status, Venue venue, Team homeTeam, Team awayTeam) {

    /**
     * Scoreboard venue. CFBD does not send {@code venueId} on this payload.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Venue(String name, String city, String state) {
    }

    /**
     * Scoreboard team. {@code cfbdTeamId} is CFBD {@code id}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Team(@JsonProperty("id") long cfbdTeamId, String name, Integer points) {
    }
}
