package com.curleesoft.pickem.backend.service;

/**
 * One row in the account theme picker. Only an active catalog key is included,
 * so choosing it always matches a compiled {@code data-theme} block.
 */
public record ThemeChoice(String id, String themeName, String themePath) {
}
