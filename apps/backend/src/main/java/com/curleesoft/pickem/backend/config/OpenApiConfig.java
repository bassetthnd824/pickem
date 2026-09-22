package com.curleesoft.pickem.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pickemOpenApi() {
        return new OpenAPI().info(new Info().title("Kenney's Pickem API").version("0.0.1")
                .description("Spring Boot REST API. The browser calls Next.js /api only; this service is not public."));
    }
}
