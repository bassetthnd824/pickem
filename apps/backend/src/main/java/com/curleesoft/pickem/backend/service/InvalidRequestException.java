package com.curleesoft.pickem.backend.service;

/**
 * A manager request failed calendar or reference validation. Mapped to HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}
