package com.curleesoft.pickem.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Runs a CFBD import off the manager request thread so
 * {@code POST /api/manager/imports/matchups} can return 202 immediately.
 * Threads are not daemons, and shutdown waits for them, so a job is not left
 * {@code running} when the process stops.
 */
@Configuration
public class ImportJobExecutorConfig {

    public static final String EXECUTOR = "importJobExecutor";

    @Bean(name = EXECUTOR)
    public ThreadPoolTaskExecutor importJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("cfbd-import-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setDaemon(false);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);
        return executor;
    }
}
