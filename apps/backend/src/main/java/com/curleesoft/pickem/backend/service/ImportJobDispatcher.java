package com.curleesoft.pickem.backend.service;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.curleesoft.pickem.backend.config.ImportJobExecutorConfig;

/**
 * Schedules {@link CfbdImportService#execute(String)} without waiting for the
 * College Football Data walk.
 */
@Slf4j
@Component
public class ImportJobDispatcher {

    private final ThreadPoolTaskExecutor importJobExecutor;

    private final CfbdImportService cfbdImportService;

    public ImportJobDispatcher(@Qualifier(ImportJobExecutorConfig.EXECUTOR) ThreadPoolTaskExecutor importJobExecutor,
            CfbdImportService cfbdImportService) {
        this.importJobExecutor = Objects.requireNonNull(importJobExecutor, "importJobExecutor");
        this.cfbdImportService = Objects.requireNonNull(cfbdImportService, "cfbdImportService");
    }

    public void dispatch(String jobId) {
        String id = Objects.requireNonNull(jobId, "jobId");
        try {
            importJobExecutor.execute(() -> run(id));
        } catch (RejectedExecutionException ex) {
            log.warn("CFBD import {} was not started: {}", id, ex.getMessage());
            cfbdImportService.recordStartFailure(id, "Import could not be started");
        }
    }

    private void run(String jobId) {
        try {
            cfbdImportService.execute(jobId);
        } catch (RuntimeException ex) {
            log.warn("CFBD import {} failed: {}", jobId, ex.getMessage());
        }
    }
}
