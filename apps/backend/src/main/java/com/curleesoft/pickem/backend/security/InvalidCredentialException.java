package com.curleesoft.pickem.backend.security;

/**
 * The Google ID token or session cookie was missing, invalid, expired, or
 * revoked. Mapped to HTTP 401.
 */
public class InvalidCredentialException extends RuntimeException {

    public InvalidCredentialException(String message) {
        super(message);
    }

    public InvalidCredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}
