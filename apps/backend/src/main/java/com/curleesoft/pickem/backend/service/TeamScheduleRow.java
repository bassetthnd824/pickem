package com.curleesoft.pickem.backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One row of {@code GET /api/game/team-schedule}. Field names match legacy
 * {@code TeamSchedule}. Unplayed games leave {@code scoreResult} and
 * {@code winLoss} null so they are omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamScheduleRow(String matchupDate, String opponentName, String scoreResult, String winLoss) {
}
