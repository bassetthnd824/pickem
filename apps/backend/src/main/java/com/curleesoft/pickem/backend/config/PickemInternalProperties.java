package com.curleesoft.pickem.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Credentials for {@code /api/internal/**}. The shared secret is never a
 * browser session. Loopback is a local stand-in for the Cloud Run identity
 * token and is off when this process is itself on Cloud Run.
 */
@ConfigurationProperties(prefix = "pickem.internal")
public record PickemInternalProperties(@DefaultValue("") String jobSecret,
        @DefaultValue("true") boolean allowLoopback) {

    public PickemInternalProperties {
        if (jobSecret == null) {
            jobSecret = "";
        } else {
            jobSecret = jobSecret.trim();
        }
    }

    public boolean allowsLoopback(String cloudRunService) {
        if (!allowLoopback) {
            return false;
        }
        return cloudRunService == null || cloudRunService.isBlank();
    }
}
