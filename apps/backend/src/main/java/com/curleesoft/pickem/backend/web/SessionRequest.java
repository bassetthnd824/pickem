package com.curleesoft.pickem.backend.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/auth/session}. Password fields are not part of the
 * contract and are ignored if a client sends them.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionRequest(@NotBlank String idToken) {
}
