package projectlx.co.zw.notifications.utils.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Loads {@code .env} from the sibling {@code ldms-config-repo} checkout (or {@code LDMS_CONFIG_REPO_DIR}).
 * Used before and after Spring Cloud Config so outbound credentials win over empty resolved placeholders.
 */
public final class LdmsConfigRepoDotenv {

    public static final String CONFIG_REPO_DIR_PROPERTY = "LDMS_CONFIG_REPO_DIR";
    public static final String PROPERTY_SOURCE_NAME = "ldmsConfigRepoDotenv";

    private LdmsConfigRepoDotenv() {
    }

    /**
     * @return map of keys to inject into Spring (only keys not already set to a non-empty OS environment value)
     */
    public static Map<String, Object> loadPropertiesForSpring() {
        Path envFile = resolveEnvFile();
        if (envFile == null) {
            return Map.of();
        }

        Dotenv dotenv = Dotenv.configure()
                .directory(envFile.getParent().toString())
                .filename(envFile.getFileName().toString())
                .ignoreIfMalformed()
                .load();

        Map<String, Object> props = new LinkedHashMap<>();
        for (DotenvEntry entry : dotenv.entries()) {
            String key = entry.getKey();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            if (hasNonEmptyOsValue(key)) {
                continue;
            }
            String value = entry.getValue();
            if (StringUtils.hasText(value)) {
                props.put(key, value);
            }
        }
        enrichWithSpringPropertyAliases(props);
        return props;
    }

    public static void applyToEnvironment(ConfigurableEnvironment environment) {
        Map<String, Object> props = loadPropertiesForSpring();
        if (props.isEmpty()) {
            return;
        }
        addOrReplacePropertySource(environment, props);
    }

    /**
     * Forces credential keys from the on-disk {@code .env} into the environment (highest priority).
     * Use after config-server import so blank {@code AWS_ACCESS_KEY_ID} from the server is overridden locally.
     */
    public static void applyCredentialOverridesFromFile(ConfigurableEnvironment environment) {
        Path envFile = resolveEnvFile();
        if (envFile == null) {
            return;
        }
        Dotenv dotenv = Dotenv.configure()
                .directory(envFile.getParent().toString())
                .filename(envFile.getFileName().toString())
                .ignoreIfMalformed()
                .load();

        Map<String, Object> overrides = new LinkedHashMap<>();
        for (DotenvEntry entry : dotenv.entries()) {
            String key = entry.getKey();
            if (!isCredentialEnvKey(key)) {
                continue;
            }
            if (!StringUtils.hasText(entry.getValue())) {
                continue;
            }
            if (hasNonEmptyOsValue(key)) {
                continue;
            }
            overrides.put(key, entry.getValue().trim());
        }
        if (overrides.isEmpty()) {
            return;
        }
        enrichWithSpringPropertyAliases(overrides);
        addOrReplacePropertySource(environment, overrides);
    }

    private static boolean isCredentialEnvKey(String key) {
        return switch (key) {
            case "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_REGION", "AWS_SES_FROM_EMAIL",
                    "SENDGRID_API_KEY", "SENDGRID_FROM_EMAIL",
                    "TWILIO_ACCOUNT_SID", "TWILIO_AUTH_TOKEN", "TWILIO_PHONE_NUMBER", "TWILIO_WHATSAPP_FROM" -> true;
            default -> false;
        };
    }

    private static void addOrReplacePropertySource(ConfigurableEnvironment environment, Map<String, Object> props) {
        if (props.isEmpty()) {
            return;
        }
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    /**
     * Empty strings in the OS environment (common in IDE run configs) must not block {@code .env} values.
     */
    static boolean hasNonEmptyOsValue(String key) {
        String envValue = System.getenv(key);
        if (StringUtils.hasText(envValue)) {
            return true;
        }
        String propertyValue = System.getProperty(key);
        return StringUtils.hasText(propertyValue);
    }

    static void enrichWithSpringPropertyAliases(Map<String, Object> props) {
        putAlias(props, "SENDGRID_API_KEY", "sendgrid.api-key");
        putAlias(props, "SENDGRID_FROM_EMAIL", "sendgrid.from-email");
        putAlias(props, "AWS_ACCESS_KEY_ID", "spring.cloud.aws.credentials.access-key");
        putAlias(props, "AWS_ACCESS_KEY_ID", "aws.credentials.access-key");
        putAlias(props, "AWS_SECRET_ACCESS_KEY", "spring.cloud.aws.credentials.secret-key");
        putAlias(props, "AWS_SECRET_ACCESS_KEY", "aws.credentials.secret-key");
        putAlias(props, "AWS_REGION", "spring.cloud.aws.region.static");
        putAlias(props, "AWS_SES_FROM_EMAIL", "aws.ses.from-email");
        putAlias(props, "TWILIO_ACCOUNT_SID", "twilio.account-sid");
        putAlias(props, "TWILIO_AUTH_TOKEN", "twilio.auth-token");
        putAlias(props, "TWILIO_PHONE_NUMBER", "twilio.phone-number");
        putAlias(props, "TWILIO_WHATSAPP_FROM", "twilio.whatsapp.from-number");
    }

    private static void putAlias(Map<String, Object> props, String envKey, String springKey) {
        Object value = props.get(envKey);
        if (value != null && StringUtils.hasText(value.toString())) {
            props.put(springKey, value);
        }
    }

    public static Path resolveEnvFile() {
        String explicit = System.getenv(CONFIG_REPO_DIR_PROPERTY);
        if (!StringUtils.hasText(explicit)) {
            explicit = System.getProperty(CONFIG_REPO_DIR_PROPERTY);
        }
        if (StringUtils.hasText(explicit)) {
            Path dir = Paths.get(explicit.trim());
            Path env = dir.resolve(".env");
            return Files.isRegularFile(env) ? env : null;
        }

        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        List<Path> candidateDirs = List.of(
                userDir.resolve("../../../ldms-config-repo"),
                userDir.resolve("../../ldms-config-repo"),
                userDir.resolve("../ldms-config-repo"),
                userDir.resolve("ldms-config-repo"),
                Paths.get(System.getProperty("user.home", ""), "IdeaProjects", "ldms-config-repo"));

        for (Path dir : candidateDirs) {
            Path normalized = dir.normalize();
            Path env = normalized.resolve(".env");
            if (Files.isRegularFile(env)) {
                return env;
            }
        }
        return null;
    }

    public static String resolvedPathForLog() {
        Path envFile = resolveEnvFile();
        return envFile != null ? envFile.toAbsolutePath().toString() : "(not found)";
    }

    public static boolean containsEmailKeys(Map<String, Object> props) {
        return Stream.of("SENDGRID_API_KEY", "sendgrid.api-key", "AWS_ACCESS_KEY_ID", "spring.cloud.aws.credentials.access-key")
                .anyMatch(props::containsKey);
    }

    public static boolean containsTwilioKeys(Map<String, Object> props) {
        return props.containsKey("TWILIO_ACCOUNT_SID") || props.containsKey("twilio.account-sid");
    }
}
