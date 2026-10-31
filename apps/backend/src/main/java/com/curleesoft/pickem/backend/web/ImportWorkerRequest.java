package com.curleesoft.pickem.backend.web;

/**
 * Optional body for {@code POST /api/internal/jobs/cfbd-matchups}. Absent,
 * the route only acknowledges the worker. With {@code jobId}, it runs that job.
 */
public record ImportWorkerRequest(String jobId) {
}
