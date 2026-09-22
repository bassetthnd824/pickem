package com.curleesoft.pickem.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RoleClaimsTests {

    @Test
    void writesPlayerClaimAndRolesMirror() {
        Map<String, Object> claims = RoleClaims.forRoles(List.of(RoleClaims.PLAYER));

        assertThat(claims).containsEntry(RoleClaims.PLAYER, true);
        assertThat(RoleClaims.read(claims)).containsExactly(RoleClaims.PLAYER);
    }

    @Test
    void readsBooleanClaimsAndRolesArray() {
        assertThat(RoleClaims.read(Map.of(RoleClaims.MANAGER, true, "extra", "ignored")))
                .containsExactly(RoleClaims.MANAGER);

        assertThat(RoleClaims.read(Map.of(RoleClaims.ROLES, List.of("manager", "nope", "player"))))
                .containsExactly(RoleClaims.PLAYER, RoleClaims.MANAGER);
    }

    @Test
    void authoritiesUseRolePrefix() {
        assertThat(RoleClaims.authorities(List.of("player", "manager")))
                .extracting(authority -> authority.getAuthority()).containsExactly("ROLE_PLAYER", "ROLE_MANAGER");
    }
}
