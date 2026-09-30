package com.curleesoft.pickem.backend.service;

import java.util.Locale;

import org.springframework.util.StringUtils;

/**
 * Shared trimming and case-insensitive contains checks for list filters.
 */
final class SearchText {

    private SearchText() {
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static String blankToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    static String normalizeContains(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
