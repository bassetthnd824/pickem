package com.curleesoft.pickem.backend.cfbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One row from CFBD {@code GET /calendar}. Dates stay the API's ISO-8601
 * timestamps.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CfbdCalendarWeek(int season, int week, String seasonType, String startDate, String endDate) {
}
