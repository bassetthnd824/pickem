package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code picks} document. One row per user per matchup.
 */
public class Pick extends AuditableDocument {

  @NotBlank
  private String userId;

  @NotBlank
  private String matchupId;

  @NotBlank
  private String seasonId;

  @NotBlank
  private String seasonWeekId;

  @NotBlank
  private String pickedTeamId;

  @NotNull
  private Integer rank;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getMatchupId() {
    return matchupId;
  }

  public void setMatchupId(String matchupId) {
    this.matchupId = matchupId;
  }

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

  public String getPickedTeamId() {
    return pickedTeamId;
  }

  public void setPickedTeamId(String pickedTeamId) {
    this.pickedTeamId = pickedTeamId;
  }

  public Integer getRank() {
    return rank;
  }

  public void setRank(Integer rank) {
    this.rank = rank;
  }
}
