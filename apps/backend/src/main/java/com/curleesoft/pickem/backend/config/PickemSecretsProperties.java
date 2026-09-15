package com.curleesoft.pickem.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pickem.secrets")
public record PickemSecretsProperties(
  boolean secretManagerEnabled,
  String cfbdApiKey
) {}
