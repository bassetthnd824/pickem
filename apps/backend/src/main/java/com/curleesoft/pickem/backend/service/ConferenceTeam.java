package com.curleesoft.pickem.backend.service;

/**
 * One row of {@code GET /api/game/teams}. The schedule dropdown needs the
 * document id and the {@code teamName squadName} label, and nothing else from
 * the team document.
 */
public record ConferenceTeam(String id, String teamName, String squadName) {
}
