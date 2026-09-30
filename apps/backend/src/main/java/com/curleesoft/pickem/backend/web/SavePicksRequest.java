package com.curleesoft.pickem.backend.web;

import java.util.List;

import com.curleesoft.pickem.backend.service.PickSelection;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/game/picks}. A client {@code userId} is not a field
 * and is ignored.
 */
public record SavePicksRequest(@NotBlank String seasonWeekId, List<PickSelection> picks) {
}