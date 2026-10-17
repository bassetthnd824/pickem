package com.curleesoft.pickem.backend.security;

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Marks a request that presented the internal job secret or was accepted on
 * loopback. A session cookie never produces this token.
 */
public class InternalJobAuthentication extends AbstractAuthenticationToken {

    public static final String AUTHORITY = "INTERNAL_JOB";

    private static final long serialVersionUID = 1L;

    public InternalJobAuthentication() {
        super(List.of(new SimpleGrantedAuthority(AUTHORITY)));
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return "internal-job";
    }
}
