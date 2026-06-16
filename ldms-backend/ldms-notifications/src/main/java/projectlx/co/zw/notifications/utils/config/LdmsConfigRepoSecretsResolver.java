package projectlx.co.zw.notifications.utils.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves outbound secrets from Spring {@link Environment} with a direct read of {@code ldms-config-repo/.env}.
 * Needed because the config server resolves {@code ${AWS_ACCESS_KEY_ID:}} to blank before the client receives YAML,
 * while {@code AWS_SES_FROM_EMAIL} often has a default in config-repo and appears to load correctly.
 */
@Component
public class LdmsConfigRepoSecretsResolver {

    private static final Logger log = LoggerFactory.getLogger(LdmsConfigRepoSecretsResolver.class);
    private static final AtomicReference<Map<String, String>> FILE_CACHE = new AtomicReference<>();

    private final Environment environment;

    public LdmsConfigRepoSecretsResolver(Environment environment) {
        this.environment = environment;
    }

    public String awsAccessKeyId() {
        return resolve(
                "AWS_ACCESS_KEY_ID",
                "spring.cloud.aws.credentials.access-key",
                "aws.credentials.access-key");
    }

    public String awsSecretAccessKey() {
        return resolve(
                "AWS_SECRET_ACCESS_KEY",
                "spring.cloud.aws.credentials.secret-key",
                "aws.credentials.secret-key");
    }

    public String awsRegion() {
        return resolve("AWS_REGION", "spring.cloud.aws.region.static");
    }

    public String awsSesFromEmail() {
        return resolve("AWS_SES_FROM_EMAIL", "aws.ses.from-email");
    }

    public String sendgridApiKey() {
        return resolve("SENDGRID_API_KEY", "sendgrid.api-key");
    }

    public String twilioAccountSid() {
        return resolve("TWILIO_ACCOUNT_SID", "twilio.account-sid");
    }

    public String twilioAuthToken() {
        return resolve("TWILIO_AUTH_TOKEN", "twilio.auth-token");
    }

    public String twilioPhoneNumber() {
        return resolve("TWILIO_PHONE_NUMBER", "twilio.phone-number");
    }

    public String twilioWhatsappFrom() {
        return resolve("TWILIO_WHATSAPP_FROM", "twilio.whatsapp.from-number");
    }

    public boolean isSesConfigured() {
        return OutboundMessagingReadiness.resolveSesReady(awsSesFromEmail(), awsAccessKeyId(), awsSecretAccessKey());
    }

    public void logCredentialPresenceAtStartup() {
        log.info(
                "[NOTIFICATION] Credential resolution (env + {}): awsAccessKeyId={} awsSecretAccessKey={} awsRegion={} "
                        + "sesFromEmail={} sesConfigured={}",
                LdmsConfigRepoDotenv.resolvedPathForLog(),
                present(awsAccessKeyId()),
                present(awsSecretAccessKey()),
                present(awsRegion()),
                maskEmail(awsSesFromEmail()),
                isSesConfigured());
    }

    private String resolve(String envKey, String... springKeys) {
        if (hasNonEmptyOsValue(envKey)) {
            return firstNonEmpty(System.getenv(envKey), System.getProperty(envKey));
        }
        for (String springKey : springKeys) {
            String fromSpring = environment.getProperty(springKey);
            if (StringUtils.hasText(fromSpring)) {
                return fromSpring.trim();
            }
        }
        String fromFile = fileEntries().get(envKey);
        return StringUtils.hasText(fromFile) ? fromFile.trim() : "";
    }

    private static Map<String, String> fileEntries() {
        Map<String, String> cached = FILE_CACHE.get();
        if (cached != null) {
            return cached;
        }
        synchronized (FILE_CACHE) {
            cached = FILE_CACHE.get();
            if (cached != null) {
                return cached;
            }
            cached = loadFileEntriesUncached();
            FILE_CACHE.set(cached);
            return cached;
        }
    }

    private static Map<String, String> loadFileEntriesUncached() {
        Path envFile = LdmsConfigRepoDotenv.resolveEnvFile();
        if (envFile == null) {
            log.warn(
                    "[NOTIFICATION] ldms-config-repo .env not found (set {} or place .env under IdeaProjects/ldms-config-repo/)",
                    LdmsConfigRepoDotenv.CONFIG_REPO_DIR_PROPERTY);
            return Map.of();
        }
        Dotenv dotenv = Dotenv.configure()
                .directory(envFile.getParent().toString())
                .filename(envFile.getFileName().toString())
                .ignoreIfMalformed()
                .load();
        Map<String, String> entries = new LinkedHashMap<>();
        for (DotenvEntry entry : dotenv.entries()) {
            if (StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
                entries.put(entry.getKey(), entry.getValue().trim());
            }
        }
        log.info("[NOTIFICATION] Read {} keys from {}", entries.size(), envFile.toAbsolutePath());
        return Map.copyOf(entries);
    }

    private static boolean hasNonEmptyOsValue(String key) {
        return StringUtils.hasText(System.getenv(key)) || StringUtils.hasText(System.getProperty(key));
    }

    private static String firstNonEmpty(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    private static String present(String value) {
        return StringUtils.hasText(value) ? "set" : "missing";
    }

    private static String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "(not set)";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** Clears cache (tests) and re-applies Spring property source from file. */
    static void resetCacheForTests() {
        FILE_CACHE.set(null);
    }
}
