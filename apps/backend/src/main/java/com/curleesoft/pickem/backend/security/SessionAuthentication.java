package com.curleesoft.pickem.backend.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication produced from a verified {@code __session} cookie.
 */
public class SessionAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final PickemPrincipal principal;

    public SessionAuthentication(PickemPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public PickemPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.uid();
    }
}
