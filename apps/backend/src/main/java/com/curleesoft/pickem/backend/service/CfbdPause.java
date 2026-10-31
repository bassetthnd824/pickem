package com.curleesoft.pickem.backend.service;

/**
 * Wait before another try of a transient College Football Data call.
 * {@code failedAttempt} is 1-based.
 */
public interface CfbdPause {

    void pause(int failedAttempt);
}
