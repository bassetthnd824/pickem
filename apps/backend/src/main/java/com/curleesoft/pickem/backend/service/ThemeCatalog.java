package com.curleesoft.pickem.backend.service;

import java.util.List;

/**
 * Compiled theme keys. CSS blocks and seed rows use this same order: Light,
 * Dark, then the 16 SEC schools. A key outside this list has no
 * {@code data-theme} palette.
 */
public final class ThemeCatalog {

    private static final List<String> KEYS = List.of(
            "light",
            "dark",
            "alabama",
            "arkansas",
            "auburn",
            "florida",
            "georgia",
            "kentucky",
            "lsu",
            "mississippi-state",
            "missouri",
            "oklahoma",
            "ole-miss",
            "south-carolina",
            "tennessee",
            "texas",
            "texas-am",
            "vanderbilt"
    );

    private ThemeCatalog() {
    }

    public static List<String> keys() {
        return KEYS;
    }

    public static boolean contains(String key) {
        return key != null && KEYS.contains(key);
    }

    public static int indexOf(String key) {

        if (key == null) {
            return -1;

        }

        return KEYS.indexOf(key);
    }
}
