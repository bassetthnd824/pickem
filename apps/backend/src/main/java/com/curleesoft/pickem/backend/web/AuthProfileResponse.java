package com.curleesoft.pickem.backend.web;

import java.util.List;

/**
 * Profile returned by {@code GET /api/auth/me}. No password fields.
 */
public record AuthProfileResponse(String uid, String emailAddr, String firstName, String lastName, String nickName,
        String themeId, List<String> roles) {
}
