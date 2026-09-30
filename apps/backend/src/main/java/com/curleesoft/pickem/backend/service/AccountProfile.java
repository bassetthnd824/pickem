package com.curleesoft.pickem.backend.service;

/**
 * {@code GET /api/game/account}. Email and name come from Google and are not
 * editable here. {@code version} is the user document's optimistic-lock value.
 */
public record AccountProfile(String emailAddr, String firstName, String lastName, String nickName, String themeId,
        Long version) {
}
