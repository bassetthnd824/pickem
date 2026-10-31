package com.curleesoft.pickem.backend.service;

import org.springframework.stereotype.Component;

import com.curleesoft.pickem.backend.cfbd.CfbdClientException;

/**
 * Exponential backoff for CFBD 429 and 5xx. The first wait is 250ms, then
 * 500ms. A third failure is not followed by another wait.
 */
@Component
class SleepingCfbdPause implements CfbdPause {

    @Override
    public void pause(int failedAttempt) {
        long delay = 250L << Math.max(0, failedAttempt - 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CfbdClientException("CFBD import interrupted");
        }
    }
}
