package com.curleesoft.pickem.backend.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Builds the Firestore client. Production uses ADC against the real project.
 * Local and tests point at the Firestore emulator and skip GCP credentials.
 */
@Configuration
public class FirestoreConfig {

  @Bean(destroyMethod = "close")
  public Firestore firestore(PickemFirestoreProperties properties) {
    String projectId = StringUtils.hasText(properties.projectId())
      ? properties.projectId()
      : "pickem-local";

    FirestoreOptions.Builder builder = FirestoreOptions.newBuilder().setProjectId(
      projectId
    );

    String emulatorHost = resolveEmulatorHost(properties);
    if (emulatorHost != null) {
      builder
        .setEmulatorHost(emulatorHost)
        .setCredentials(new FirestoreOptions.EmulatorCredentials());
    }

    return builder.build().getService();
  }

  static String resolveEmulatorHost(PickemFirestoreProperties properties) {
    String fromEnv = System.getenv("FIRESTORE_EMULATOR_HOST");
    if (!StringUtils.hasText(fromEnv)) {
      fromEnv = System.getProperty("FIRESTORE_EMULATOR_HOST");
    }
    if (StringUtils.hasText(fromEnv)) {
      return fromEnv.trim();
    }
    if (properties.emulatorEnabled() && StringUtils.hasText(properties.emulatorHost())) {
      return properties.emulatorHost().trim();
    }
    return null;
  }
}
