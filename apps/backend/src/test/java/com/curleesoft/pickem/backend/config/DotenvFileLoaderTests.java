package com.curleesoft.pickem.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.curleesoft.pickem.backend.config.DotenvFileLoader.LoadedDotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotenvFileLoaderTests {

  @AfterEach
  void clearExplicitPath() {
    System.clearProperty(DotenvFileLoader.DOTENV_PATH_PROPERTY);
  }

  @Test
  void loadsKeyValuePairsFromDotenvFile(@TempDir Path tempDir) throws Exception {
    Path envFile = tempDir.resolve(".env");
    Files.writeString(
      envFile,
      """
      # comment
      CFBD_API_KEY=from-file
      EMPTY_VALUE=
      """
    );
    System.setProperty(
      DotenvFileLoader.DOTENV_PATH_PROPERTY,
      envFile.toString()
    );

    LoadedDotenv loaded = DotenvFileLoader.load();

    assertThat(loaded.present()).isTrue();
    assertThat(loaded.file()).isEqualTo(envFile.toAbsolutePath().normalize());
    assertThat(loaded.values()).containsEntry("CFBD_API_KEY", "from-file");
  }

  @Test
  void returnsMissingWhenNoFileExists(@TempDir Path tempDir) {
    System.setProperty(
      DotenvFileLoader.DOTENV_PATH_PROPERTY,
      tempDir.resolve("does-not-exist.env").toString()
    );

    LoadedDotenv loaded = DotenvFileLoader.load();

    assertThat(loaded.present()).isFalse();
    assertThat(loaded.values()).isEmpty();
  }
}
