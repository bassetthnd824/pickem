package com.curleesoft.pickem.backend.security;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.curleesoft.pickem.backend.config.PickemAuthProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Host-only {@code __session} cookie. No {@code Domain} attribute, so the
 * browser keeps it on the host that set it (the Next.js origin after the BFF
 * copies {@code Set-Cookie}).
 */
@Component
public class SessionCookies {

    public static final String NAME = "__session";

    private final Duration maxAge;

    public SessionCookies(PickemAuthProperties properties) {
        this.maxAge = properties.sessionDuration();
    }

    public ResponseCookie issue(String value) {
        return base(value).maxAge(maxAge).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (cookie == null || !NAME.equals(cookie.getName())) {
                continue;
            }
            String value = cookie.getValue();
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value == null ? "" : value).httpOnly(true).secure(true).path("/")
                .sameSite("Lax");
    }
}
