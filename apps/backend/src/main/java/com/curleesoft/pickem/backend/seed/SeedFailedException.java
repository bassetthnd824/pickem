package com.curleesoft.pickem.backend.seed;

/**
 * The seed could not finish. Reference data may already have been written.
 */
public class SeedFailedException extends RuntimeException {

    public SeedFailedException(String message) {
        super(message);
    }

    public SeedFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
