package com.curleesoft.pickem.backend.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for {@code POST /api/manager/imports/matchups}. {@code seasonType}
 * defaults to {@code regular} when omitted.
 */
public record ImportMatchupsRequest(@NotBlank String seasonId, String seasonType) {
}
