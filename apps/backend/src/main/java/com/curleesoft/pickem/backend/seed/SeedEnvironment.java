package com.curleesoft.pickem.backend.seed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

/**
 * Process environment for {@link SeedMain}. OS variables win over a dotenv
 * file. {@code --email} wins over {@code PICKEM_BOOTSTRAP_MANAGER_EMAIL}.
 */
public record SeedEnvironment(String projectId, String firestoreEmulatorHost, String authEmulatorHost,
        String bootstrapEmail) {

    static final String DOTENV_PATH = "PICKEM_DOTENV_FILE";

    static final String PROJECT_ID = "GOOGLE_CLOUD_PROJECT";

    static final String FIRESTORE_HOST = "FIRESTORE_EMULATOR_HOST";

    static final String AUTH_HOST = "FIREBASE_AUTH_EMULATOR_HOST";

    static final String BOOTSTRAP_EMAIL = "PICKEM_BOOTSTRAP_MANAGER_EMAIL";

    private static final String DEFAULT_PROJECT_ID = "pickem-local";

    public static SeedEnvironment load(String[] args) {
        Map<String, String> environment = new LinkedHashMap<>(System.getenv());
        copyPropertyIfAbsent(environment, DOTENV_PATH);
        copyPropertyIfAbsent(environment, PROJECT_ID);
        copyPropertyIfAbsent(environment, FIRESTORE_HOST);
        copyPropertyIfAbsent(environment, AUTH_HOST);
        copyPropertyIfAbsent(environment, BOOTSTRAP_EMAIL);
        Path cwd = Path.of("").toAbsolutePath().normalize();
        return from(environment, dotenvValues(environment, cwd), args);
    }

    public static SeedEnvironment from(Map<String, String> environment, Map<String, String> dotenv, String[] args) {
        Map<String, String> env = environment == null ? Map.of() : environment;
        Map<String, String> file = dotenv == null ? Map.of() : dotenv;
        String project = value(env, file, PROJECT_ID);
        String emailFlag = emailArg(args);
        String email = emailFlag != null ? blankToNull(emailFlag) : value(env, file, BOOTSTRAP_EMAIL);

        return new SeedEnvironment(project == null ? DEFAULT_PROJECT_ID : project, value(env, file, FIRESTORE_HOST),
                value(env, file, AUTH_HOST), email);
    }

    static Map<String, String> dotenvValues(Map<String, String> environment, Path cwd) {
        Path file = resolveDotenv(environment == null ? Map.of() : environment, cwd);

        if (file == null) {
            return Map.of();
        }

        Dotenv dotenv = Dotenv.configure().directory(file.getParent().toString()).filename(file.getFileName().toString())
                .ignoreIfMalformed().ignoreIfMissing().load();
        Map<String, String> values = new LinkedHashMap<>();

        for (DotenvEntry entry : dotenv.entries()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }

            if (environment != null && environment.get(entry.getKey()) != null) {
                continue;
            }

            values.put(entry.getKey(), entry.getValue());
        }

        return values;
    }

    private static Path resolveDotenv(Map<String, String> environment, Path cwd) {
        String explicit = environment.get(DOTENV_PATH);

        if (explicit != null && !explicit.isBlank()) {
            Path configured = Path.of(explicit.trim());

            if (!Files.isRegularFile(configured)) {
                return null;
            }

            return configured.toAbsolutePath().normalize();
        }

        Path base = cwd == null ? Path.of("").toAbsolutePath().normalize() : cwd.toAbsolutePath().normalize();
        Path[] candidates = new Path[] { base.resolve(".env"), base.resolve("apps").resolve("backend").resolve(".env"),
                base.getParent() == null ? null : base.getParent().resolve(".env") };

        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        return null;
    }

    private static void copyPropertyIfAbsent(Map<String, String> environment, String key) {
        if (environment.get(key) != null) {
            return;
        }

        String property = System.getProperty(key);

        if (property != null) {
            environment.put(key, property);
        }
    }

    private static String value(Map<String, String> environment, Map<String, String> dotenv, String key) {
        if (environment.get(key) != null) {
            return blankToNull(environment.get(key));
        }

        return blankToNull(dotenv.get(key));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static String emailArg(String[] args) {
        if (args == null) {
            return null;
        }

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];

            if (arg == null) {
                continue;
            }

            if (arg.startsWith("--email=")) {
                return arg.substring("--email=".length()).trim();
            }

            if ("--email".equals(arg) && index + 1 < args.length) {
                return args[index + 1] == null ? "" : args[index + 1].trim();
            }
        }

        return null;
    }
}
