package com.curleesoft.pickem.backend.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Firebase custom claims for {@code player} and {@code manager}, mirrored by
 * {@code users.roles}.
 */
public final class RoleClaims {

    public static final String PLAYER = "player";

    public static final String MANAGER = "manager";

    public static final String ROLES = "roles";

    private RoleClaims() {
    }

    public static Map<String, Object> forRoles(Collection<String> roles) {
        List<String> normalized = normalize(roles);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(ROLES, normalized);
        for (String role : normalized) {
            claims.put(role, true);
        }
        return claims;
    }

    public static List<String> read(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(claims.get(PLAYER))) {
            roles.add(PLAYER);
        }
        if (Boolean.TRUE.equals(claims.get(MANAGER))) {
            roles.add(MANAGER);
        }
        Object rawRoles = claims.get(ROLES);
        if (rawRoles instanceof Collection<?> collection) {
            for (Object role : collection) {
                if (role instanceof String text) {
                    addIfKnown(roles, text);
                }
            }
        }
        return ordered(roles);
    }

    public static List<String> normalize(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String role : roles) {
            addIfKnown(normalized, role);
        }
        return ordered(normalized);
    }

    public static List<GrantedAuthority> authorities(Collection<String> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : normalize(roles)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
        }
        return List.copyOf(authorities);
    }

    private static List<String> ordered(LinkedHashSet<String> roles) {
        List<String> ordered = new ArrayList<>();
        if (roles.contains(PLAYER)) {
            ordered.add(PLAYER);
        }
        if (roles.contains(MANAGER)) {
            ordered.add(MANAGER);
        }
        return List.copyOf(ordered);
    }

    private static void addIfKnown(LinkedHashSet<String> roles, String role) {
        if (role == null) {
            return;
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (PLAYER.equals(normalized) || MANAGER.equals(normalized)) {
            roles.add(normalized);
        }
    }
}
