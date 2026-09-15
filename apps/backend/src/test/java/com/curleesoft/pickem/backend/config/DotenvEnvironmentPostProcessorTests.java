package com.curleesoft.pickem.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

class DotenvEnvironmentPostProcessorTests {

  @AfterEach
  void clearExplicitPath() {
    System.clearProperty(DotenvFileLoader.DOTENV_PATH_PROPERTY);
  }

  @Test
  void addsDotenvValuesToTheEnvironment(@TempDir Path tempDir) throws Exception {
    Path envFile = tempDir.resolve(".env");
    Files.writeString(envFile, "CFBD_API_KEY=from-dotenv\n");
    System.setProperty(
      DotenvFileLoader.DOTENV_PATH_PROPERTY,
      envFile.toString()
    );

    StandardEnvironment environment = new StandardEnvironment();
    new DotenvEnvironmentPostProcessor()
      .postProcessEnvironment(environment, new SpringApplication());

    assertThat(
      environment
        .getPropertySources()
        .contains(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)
    ).isTrue();
    assertThat(environment.getProperty("CFBD_API_KEY")).isEqualTo("from-dotenv");
  }
}
