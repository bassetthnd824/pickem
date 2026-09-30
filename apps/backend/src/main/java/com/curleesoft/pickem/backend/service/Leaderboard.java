package com.curleesoft.pickem.backend.service;

import java.util.List;

/**
 * {@code GET /api/game/leaderboard}. Every user is listed, including players
 * with no picks. {@code score} sums confidence ranks for correct picks in weeks
 * that have begun. Equal scores break by {@code uid} ascending (legacy
 * {@code User.compareTo} on userId), and {@code rank} is the 1-based place in
 * that order.
 */
public record Leaderboard(String seasonId, String season, List<Standing> standings) {

    public record Standing(int rank, String uid, String nickName, long score) {
    }
}
