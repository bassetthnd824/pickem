package com.curleesoft.pickem.backend.security;

import java.io.Serializable;
import java.util.List;

/**
 * Authenticated Firebase user. {@code uid} is the security name stamped on
 * audit fields.
 */
public record PickemPrincipal(String uid, List<String> roles) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PickemPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
