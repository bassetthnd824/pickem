package com.curleesoft.pickem.backend.security;

/**
 * Firebase Admin could not be initialized. Mapped to HTTP 503.
 */
public class AuthUnavailableException extends RuntimeException {

    public AuthUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
