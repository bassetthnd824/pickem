package com.curleesoft.pickem.backend.service;

/**
 * The requested document does not exist. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
