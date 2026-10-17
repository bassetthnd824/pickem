package com.curleesoft.pickem.backend.model;

import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code matchups} document. Embeds home/away team and venue snapshots, the
 * week number, and an optional rivalry name. Those snapshots, {@code weekNumber},
 * {@code rivalryName}, and {@code winningTeamId} are written by the service and
 * may be omitted on input. {@code winningTeamId} is read-only: a client value is
 * ignored and the service derives it from the scores.
 */
@Getter
@Setter
public class Matchup extends AuditableDocument {

    @NotBlank
    private String seasonId;

    @NotBlank
    private String seasonWeekId;

    private Integer weekNumber;

    @NotBlank
    private String matchupDate;

    @NotBlank
    private String homeTeamId;

    @NotBlank
    private String awayTeamId;

    private TeamSquadSnapshot homeTeam;

    private TeamSquadSnapshot awayTeam;

    private Integer homeTeamScore;
    private Integer awayTeamScore;

    @Getter(onMethod_ = @JsonProperty(access = JsonProperty.Access.READ_ONLY))
    private String winningTeamId;

    @NotBlank
    private String venueId;

    private VenueSnapshot venue;

    private String rivalryName;
    private Long cfbdGameId;
}
