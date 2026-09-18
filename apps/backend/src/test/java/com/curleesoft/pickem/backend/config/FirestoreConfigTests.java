package com.curleesoft.pickem.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FirestoreConfigTests {

  @Test
  void prefersFireStoreEmulatorHostEnvironmentOverProperties() {
    PickemFirestoreProperties properties = new PickemFirestoreProperties(
      "pickem-local",
      true,
      "127.0.0.1:9999"
    );

    String host = FirestoreConfig.resolveEmulatorHost(properties);

    if (System.getenv("FIRESTORE_EMULATOR_HOST") != null) {
      assertThat(host).isEqualTo(System.getenv("FIRESTORE_EMULATOR_HOST"));
    } else {
      assertThat(host).isEqualTo("127.0.0.1:9999");
    }
  }

  @Test
  void returnsNullWhenEmulatorDisabledAndNoEnv() {
    if (System.getenv("FIRESTORE_EMULATOR_HOST") != null) {
      return;
    }
    PickemFirestoreProperties properties = new PickemFirestoreProperties(
      "prod-project",
      false,
      ""
    );

    assertThat(FirestoreConfig.resolveEmulatorHost(properties)).isNull();
  }
}
