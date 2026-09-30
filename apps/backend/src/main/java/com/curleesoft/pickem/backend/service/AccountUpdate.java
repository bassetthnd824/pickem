package com.curleesoft.pickem.backend.service;

/**
 * Fields a player may change on {@code PUT /api/game/account}. Email, name,
 * and roles are not part of this command, so a client cannot send them through.
 */
public record AccountUpdate(String nickName, String themeId, Long version) {
}
