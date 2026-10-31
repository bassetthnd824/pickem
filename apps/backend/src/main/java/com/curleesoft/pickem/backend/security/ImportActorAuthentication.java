package com.curleesoft.pickem.backend.security;

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Audit actor for CFBD import writes. The worker thread has no manager
 * session, and imported documents are stamped {@code cfbd-import}.
 */
public class ImportActorAuthentication extends AbstractAuthenticationToken {

    public static final String ACTOR = "cfbd-import";

    private static final long serialVersionUID = 1L;

    public ImportActorAuthentication() {
        super(List.of());
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return ACTOR;
    }
}
