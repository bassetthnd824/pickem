package com.curleesoft.pickem.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pickem.firestore")
public record PickemFirestoreProperties(
  @DefaultValue("pickem-local") String projectId,
  @DefaultValue("false") boolean emulatorEnabled,
  String emulatorHost
) {}
