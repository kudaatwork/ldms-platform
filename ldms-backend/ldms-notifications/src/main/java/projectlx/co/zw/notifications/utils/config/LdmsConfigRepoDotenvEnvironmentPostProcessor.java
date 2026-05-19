package projectlx.co.zw.notifications.utils.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Loads {@code ldms-config-repo/.env} before Spring Cloud Config resolves {@code ${SENDGRID_API_KEY:}} placeholders.
 */
public class LdmsConfigRepoDotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LdmsConfigRepoDotenvEnvironmentPostProcessor.class);

    /** Runs immediately before {@code ConfigDataEnvironmentPostProcessor} (+10). */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 9;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = LdmsConfigRepoDotenv.loadPropertiesForSpring();
        if (props.isEmpty()) {
            log.warn(
                    "[NOTIFICATION] No ldms-config-repo .env loaded (checked {}). "
                            + "Set {} to your config repo path or place .env in IdeaProjects/ldms-config-repo/.env",
                    LdmsConfigRepoDotenv.resolvedPathForLog(),
                    LdmsConfigRepoDotenv.CONFIG_REPO_DIR_PROPERTY);
            return;
        }

        LdmsConfigRepoDotenv.applyToEnvironment(environment);
        log.info(
                "[NOTIFICATION] Loaded {} entries from ldms-config-repo .env ({}) emailKeysPresent={} twilioKeysPresent={}",
                props.size(),
                LdmsConfigRepoDotenv.resolvedPathForLog(),
                LdmsConfigRepoDotenv.containsEmailKeys(props),
                LdmsConfigRepoDotenv.containsTwilioKeys(props));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
