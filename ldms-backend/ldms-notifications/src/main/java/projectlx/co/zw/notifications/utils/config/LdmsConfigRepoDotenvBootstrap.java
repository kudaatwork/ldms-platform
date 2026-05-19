package projectlx.co.zw.notifications.utils.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Re-applies {@code ldms-config-repo/.env} after Spring Cloud Config data is loaded.
 * Registered via {@code META-INF/spring/org.springframework.context.ApplicationListener} (not a {@code @Component})
 * so it runs before the application context is refreshed.
 */
public class LdmsConfigRepoDotenvBootstrap implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        LdmsConfigRepoDotenv.applyCredentialOverridesFromFile(environment);
    }
}
