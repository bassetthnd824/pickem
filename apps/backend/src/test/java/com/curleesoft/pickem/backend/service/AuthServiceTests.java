package com.curleesoft.pickem.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthServiceTests {

    @Test
    void nickNameDefaultsToFirstNameTruncatedAtFortyCharacters() {
        assertThat(AuthService.nickName("Kenney")).isEqualTo("Kenney");
        assertThat(AuthService.nickName("x".repeat(50))).hasSize(40);
    }
}
