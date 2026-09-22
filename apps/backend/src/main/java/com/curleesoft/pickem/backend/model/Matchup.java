package com.curleesoft.pickem.backend.model;

import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code matchups} document. Embeds home/away team and venue snapshots.
 * {@code winningTeamId} is computed server-side in later stories.
 */
public class Matchup extends AuditableDocument {

    @NotBlank
    private String seasonId;

    @NotBlank
    private String seasonWeekId;

    @NotNull
    private Integer weekNumber;

    @NotBlank
    private String matchupDate;

    @NotBlank
    private String homeTeamId;

    @NotBlank
    private String awayTeamId;

    @NotNull
    @Valid
    private TeamSquadSnapshot homeTeam;

    @NotNull
    @Valid
    private TeamSquadSnapshot awayTeam;

    private Integer homeTeamScore;
    private Integer awayTeamScore;
    private String winningTeamId;

    @NotBlank
    private String venueId;

    @NotNull
    @Valid
    private VenueSnapshot venue;

    private String rivalryName;
    private Long cfbdGameId;

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public String getSeasonWeekId() {
        return seasonWeekId;
    }

    public void setSeasonWeekId(String seasonWeekId) {
        this.seasonWeekId = seasonWeekId;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getMatchupDate() {
        return matchupDate;
    }

    public void setMatchupDate(String matchupDate) {
        this.matchupDate = matchupDate;
    }

    public String getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(String homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public String getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(String awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    public TeamSquadSnapshot getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(TeamSquadSnapshot homeTeam) {
        this.homeTeam = homeTeam;
    }

    public TeamSquadSnapshot getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(TeamSquadSnapshot awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Integer getHomeTeamScore() {
        return homeTeamScore;
    }

    public void setHomeTeamScore(Integer homeTeamScore) {
        this.homeTeamScore = homeTeamScore;
    }

    public Integer getAwayTeamScore() {
        return awayTeamScore;
    }

    public void setAwayTeamScore(Integer awayTeamScore) {
        this.awayTeamScore = awayTeamScore;
    }

    public String getWinningTeamId() {
        return winningTeamId;
    }

    public void setWinningTeamId(String winningTeamId) {
        this.winningTeamId = winningTeamId;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public VenueSnapshot getVenue() {
        return venue;
    }

    public void setVenue(VenueSnapshot venue) {
        this.venue = venue;
    }

    public String getRivalryName() {
        return rivalryName;
    }

    public void setRivalryName(String rivalryName) {
        this.rivalryName = rivalryName;
    }

    public Long getCfbdGameId() {
        return cfbdGameId;
    }

    public void setCfbdGameId(Long cfbdGameId) {
        this.cfbdGameId = cfbdGameId;
    }
}
