package com.curleesoft.pickem.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PickemSecretsPropertiesTests extends FirestoreEmulatorSupport {

  @Autowired
  private PickemSecretsProperties secrets;

  @Test
  void bindsTestSecretsWithoutGoogleCloud() {
    assertThat(secrets.secretManagerEnabled()).isFalse();
    assertThat(secrets.cfbdApiKey()).isEqualTo("test-cfbd-api-key");
  }
}
