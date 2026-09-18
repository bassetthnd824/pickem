package com.curleesoft.pickem.backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code seasonWeeks} document. Calendar dates are ISO-8601 {@code YYYY-MM-DD}.
 */
public class SeasonWeek extends AuditableDocument {

  @NotBlank
  private String seasonId;

  @NotNull
  private Integer weekNumber;

  @NotBlank
  private String beginDate;

  @NotBlank
  private String endDate;

  public String getSeasonId() {
    return seasonId;
  }

  public void setSeasonId(String seasonId) {
    this.seasonId = seasonId;
  }

  public Integer getWeekNumber() {
    return weekNumber;
  }

  public void setWeekNumber(Integer weekNumber) {
    this.weekNumber = weekNumber;
  }

  public String getBeginDate() {
    return beginDate;
  }

  public void setBeginDate(String beginDate) {
    this.beginDate = beginDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }
}
