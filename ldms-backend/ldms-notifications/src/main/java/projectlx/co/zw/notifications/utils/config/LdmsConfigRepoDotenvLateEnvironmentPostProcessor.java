package projectlx.co.zw.notifications.utils.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Re-applies {@code ldms-config-repo/.env} after Spring Cloud Config so non-empty local secrets override
 * empty {@code sendgrid.api-key} / {@code twilio.*} values resolved from the config server.
 */
public class LdmsConfigRepoDotenvLateEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LdmsConfigRepoDotenvLateEnvironmentPostProcessor.class);

    /** Runs immediately after {@code ConfigDataEnvironmentPostProcessor} (+10). */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 11;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = LdmsConfigRepoDotenv.loadPropertiesForSpring();
        if (props.isEmpty()) {
            return;
        }
        LdmsConfigRepoDotenv.applyToEnvironment(environment);
        LdmsConfigRepoDotenv.applyCredentialOverridesFromFile(environment);
        log.debug(
                "[NOTIFICATION] Re-applied ldms-config-repo .env after config import (emailKeysPresent={} twilioKeysPresent={})",
                LdmsConfigRepoDotenv.containsEmailKeys(props),
                LdmsConfigRepoDotenv.containsTwilioKeys(props));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
