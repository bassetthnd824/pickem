package com.curleesoft.pickem.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pickem.cfbd")
public record PickemCfbdProperties(String baseUrl) {

    public static final String DEFAULT_BASE_URL = "https://api.collegefootballdata.com";
}
