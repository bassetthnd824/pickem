package com.curleesoft.pickem.backend.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SeedEnvironmentTests {

    @Test
    void emailFlagOverridesTheEnvironment() {
        SeedEnvironment environment = SeedEnvironment.from(
                Map.of(SeedEnvironment.BOOTSTRAP_EMAIL, "env@example.com", SeedEnvironment.PROJECT_ID, "proj"),
                Map.of(SeedEnvironment.BOOTSTRAP_EMAIL, "file@example.com"),
                new String[] { "--email=cli@example.com" });

        assertThat(environment.bootstrapEmail()).isEqualTo("cli@example.com");
        assertThat(environment.projectId()).isEqualTo("proj");
    }

    @Test
    void spacedEmailFlagOverridesTheEnvironment() {
        SeedEnvironment environment = SeedEnvironment.from(Map.of(SeedEnvironment.BOOTSTRAP_EMAIL, "env@example.com"),
                Map.of(), new String[] { "--email", "cli@example.com" });

        assertThat(environment.bootstrapEmail()).isEqualTo("cli@example.com");
    }

    @Test
    void osValueWinsOverDotenv() {
        SeedEnvironment environment = SeedEnvironment.from(Map.of(SeedEnvironment.FIRESTORE_HOST, "127.0.0.1:8085"),
                Map.of(SeedEnvironment.FIRESTORE_HOST, "10.0.0.1:8085", SeedEnvironment.PROJECT_ID, "from-file",
                        SeedEnvironment.AUTH_HOST, "127.0.0.1:9099"),
                new String[0]);

        assertThat(environment.firestoreEmulatorHost()).isEqualTo("127.0.0.1:8085");
        assertThat(environment.projectId()).isEqualTo("from-file");
        assertThat(environment.authEmulatorHost()).isEqualTo("127.0.0.1:9099");
    }

    @Test
    void blankOsValueBlocksTheDotenvValue() {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put(SeedEnvironment.BOOTSTRAP_EMAIL, "  ");

        SeedEnvironment resolved = SeedEnvironment.from(environment,
                Map.of(SeedEnvironment.BOOTSTRAP_EMAIL, "file@example.com"), new String[0]);

        assertThat(resolved.bootstrapEmail()).isNull();
        assertThat(resolved.projectId()).isEqualTo("pickem-local");
        assertThat(resolved.firestoreEmulatorHost()).isNull();
    }

    @Test
    void blankEmailFlagSkipsTheManager() {
        SeedEnvironment environment = SeedEnvironment.from(Map.of(SeedEnvironment.BOOTSTRAP_EMAIL, "env@example.com"),
                Map.of(), new String[] { "--email=" });

        assertThat(environment.bootstrapEmail()).isNull();
    }

    @Test
    void dotenvFileSkipsKeysAlreadyInTheEnvironment() throws Exception {
        Path dir = Files.createTempDirectory("pickem-seed-env");
        Files.writeString(dir.resolve(".env"), "GOOGLE_CLOUD_PROJECT=from-file\nFIRESTORE_EMULATOR_HOST=127.0.0.1:8085\n"
                + "PICKEM_BOOTSTRAP_MANAGER_EMAIL=file@example.com\n");
        Map<String, String> os = Map.of(SeedEnvironment.PROJECT_ID, "from-os");
        Map<String, String> dotenv = SeedEnvironment.dotenvValues(os, dir);

        assertThat(dotenv).doesNotContainKey(SeedEnvironment.PROJECT_ID);
        assertThat(dotenv).containsEntry(SeedEnvironment.FIRESTORE_HOST, "127.0.0.1:8085");

        SeedEnvironment environment = SeedEnvironment.from(os, dotenv, new String[0]);
        assertThat(environment.projectId()).isEqualTo("from-os");
        assertThat(environment.firestoreEmulatorHost()).isEqualTo("127.0.0.1:8085");
        assertThat(environment.bootstrapEmail()).isEqualTo("file@example.com");
    }

    @Test
    void missingExplicitDotenvFileDoesNotFallThrough() throws Exception {
        Path dir = Files.createTempDirectory("pickem-seed-missing");
        Files.writeString(dir.resolve(".env"), "GOOGLE_CLOUD_PROJECT=from-cwd\n");
        Map<String, String> os = Map.of(SeedEnvironment.DOTENV_PATH, dir.resolve("missing.env").toString());

        assertThat(SeedEnvironment.dotenvValues(os, dir)).isEmpty();
    }
}
