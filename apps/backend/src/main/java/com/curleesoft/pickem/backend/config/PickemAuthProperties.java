package com.curleesoft.pickem.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Session-cookie settings. {@code sessionDuration} must stay inside the
 * Firebase Admin window of 5 minutes through 14 days.
 */
@ConfigurationProperties(prefix = "pickem.auth")
public record PickemAuthProperties(@DefaultValue("firebase") String provider,
        @DefaultValue("P14D") Duration sessionDuration, @DefaultValue("") String emulatorHost) {
    public PickemAuthProperties {
        if (provider == null || provider.isBlank()) {
            provider = "firebase";
        }
        if (emulatorHost == null) {
            emulatorHost = "";
        }
        if (sessionDuration == null || sessionDuration.compareTo(Duration.ofMinutes(5)) < 0
                || sessionDuration.compareTo(Duration.ofDays(14)) > 0) {
            throw new IllegalArgumentException("pickem.auth.session-duration must be between 5 minutes and 14 days");
        }
    }
}
