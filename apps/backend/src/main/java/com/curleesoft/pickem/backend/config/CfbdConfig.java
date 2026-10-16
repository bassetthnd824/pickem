package com.curleesoft.pickem.backend.config;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.curleesoft.pickem.backend.cfbd.CfbdClient;

import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the College Football Data client. The key comes from
 * {@code CFBD_API_KEY} or Secret Manager ({@code sm@cfbd-api-key}). A blank key
 * does not stop startup; {@link CfbdClient} fails the first call.
 */
@Configuration
public class CfbdConfig {

    private static final Logger log = LoggerFactory.getLogger(CfbdConfig.class);

    @Bean
    public CfbdClient cfbdClient(JsonMapper jsonMapper, PickemCfbdProperties cfbd, PickemSecretsProperties secrets) {
        JsonMapper mapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
        PickemCfbdProperties properties = Objects.requireNonNull(cfbd, "cfbd");
        PickemSecretsProperties secretProperties = Objects.requireNonNull(secrets, "secrets");
        String baseUrl = resolveBaseUrl(properties.baseUrl());
        if (!StringUtils.hasText(secretProperties.cfbdApiKey())) {
            log.warn("CFBD API key is not configured; College Football Data calls will fail");
        }
        RestClient http = Objects.requireNonNull(RestClient.builder().baseUrl(baseUrl).configureMessageConverters(
                converters -> jsonConverter(converters, mapper)).build(), "restClient");
        return new CfbdClient(http, secretProperties.cfbdApiKey());
    }

    private static void jsonConverter(HttpMessageConverters.ClientBuilder converters, JsonMapper mapper) {
        HttpMessageConverters.ClientBuilder builder = Objects.requireNonNull(converters, "converters");
        JsonMapper json = Objects.requireNonNull(mapper, "jsonMapper");
        builder.disableDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(json));
    }

    private static String resolveBaseUrl(String configured) {
        String baseUrl = StringUtils.hasText(configured) ? configured.trim() : PickemCfbdProperties.DEFAULT_BASE_URL;
        String resolved = Objects.requireNonNull(baseUrl, "baseUrl");
        if (resolved.endsWith("/")) {
            resolved = resolved.substring(0, resolved.length() - 1);
        }
        return resolved;
    }
}
