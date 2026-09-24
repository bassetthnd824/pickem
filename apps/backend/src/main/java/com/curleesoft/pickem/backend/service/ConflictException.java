package com.curleesoft.pickem.backend.service;

/**
 * The write conflicts with an existing unique value. Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
