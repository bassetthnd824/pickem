package com.curleesoft.pickem.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a dotenv file if one exists. Existing OS environment variables win over
 * values from the file.
 */
final class DotenvFileLoader {

  static final String DOTENV_PATH_PROPERTY = "PICKEM_DOTENV_FILE";

  private DotenvFileLoader() {}

  static LoadedDotenv load() {
    Path file = resolveFile();
    if (file == null) {
      return LoadedDotenv.missing();
    }

    Dotenv dotenv = Dotenv.configure()
      .directory(file.getParent().toString())
      .filename(file.getFileName().toString())
      .ignoreIfMalformed()
      .ignoreIfMissing()
      .load();

    Map<String, String> values = new LinkedHashMap<>();
    for (DotenvEntry entry : dotenv.entries()) {
      if (entry.getKey() == null || entry.getKey().isBlank()) {
        continue;
      }
      if (entry.getValue() == null) {
        continue;
      }
      if (System.getenv(entry.getKey()) != null) {
        continue;
      }
      values.put(entry.getKey(), entry.getValue());
    }
    return new LoadedDotenv(file, values);
  }

  private static Path resolveFile() {
    List<Path> candidates = new ArrayList<>();
    String explicit = System.getenv(DOTENV_PATH_PROPERTY);
    if (explicit == null || explicit.isBlank()) {
      explicit = System.getProperty(DOTENV_PATH_PROPERTY);
    }
    if (explicit != null && !explicit.isBlank()) {
      candidates.add(Path.of(explicit));
    }

    Path cwd = Path.of("").toAbsolutePath().normalize();
    candidates.add(cwd.resolve(".env"));
    candidates.add(cwd.resolve("apps/backend/.env"));
    if (cwd.getParent() != null) {
      candidates.add(cwd.getParent().resolve(".env"));
    }

    for (Path candidate : candidates) {
      if (Files.isRegularFile(candidate)) {
        return candidate.toAbsolutePath().normalize();
      }
    }
    return null;
  }

  record LoadedDotenv(Path file, Map<String, String> values) {
    boolean present() {
      return file != null;
    }

    static LoadedDotenv missing() {
      return new LoadedDotenv(null, Map.of());
    }
  }
}
