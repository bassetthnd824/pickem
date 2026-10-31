package com.curleesoft.pickem.backend.web;

import java.util.Objects;

/**
 * 202 body. The College Football Data walk continues on the worker.
 */
public record ImportAccepted(String jobId) {

    public ImportAccepted {
        Objects.requireNonNull(jobId, "jobId");
    }
}
