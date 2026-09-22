package com.curleesoft.pickem.backend.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.curleesoft.pickem.backend.config.DotenvFileLoader.LoadedDotenv;

/**
 * Loads {@code .env} into the Spring Environment before application.yml is
 * processed. OS environment variables take precedence over the file. Google
 * Secret Manager ({@code sm://}) is a separate, optional config-data source.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "pickemDotenv";

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        LoadedDotenv loaded = DotenvFileLoader.load();
        if (!loaded.present()) {
            log.debug("No .env file found; skipping dotenv property source");
            return;
        }

        Map<String, Object> source = Map.copyOf(loaded.values());
        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, source);
        if (environment.getPropertySources().contains("systemEnvironment")) {
            environment.getPropertySources().addAfter("systemEnvironment", propertySource);
        } else {
            environment.getPropertySources().addLast(propertySource);
        }
        log.info("Loaded {} secret(s) from {} (existing environment variables were not overwritten)", source.size(),
                loaded.file());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
