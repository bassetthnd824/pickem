package com.curleesoft.pickem.backend.service;

/**
 * One row of {@code POST /api/game/picks}. A blank {@code pickedTeamId} drops
 * the pick. There is no user id: the save always belongs to the caller.
 */
public record PickSelection(String matchupId, String pickedTeamId, Integer rank) {
}