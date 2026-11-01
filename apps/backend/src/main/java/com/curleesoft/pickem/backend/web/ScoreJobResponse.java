package com.curleesoft.pickem.backend.web;

import java.util.Objects;

/**
 * Finished game-day score sync. {@code updated} is how many matchups changed.
 */
public record ScoreJobResponse(String job, int updated) {

    public ScoreJobResponse {
        Objects.requireNonNull(job, "job");
    }
}
