package projectlx.user.authentication.service.utils.config;

import org.flywaydb.core.api.FlywayException;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures Flyway baselines Hibernate-created {@code ldms_auth_management} schemas before applying
 * migrations, even when Spring Cloud Config overrides local {@code application.yml} Flyway flags.
 */
@Configuration(proxyBeanMethods = false)
public class AuthenticationFlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer authenticationFlywayCustomizer() {
        return configuration -> configuration
                .baselineOnMigrate(true)
                .baselineVersion("0");
    }

    /**
     * Fallback when {@code baselineOnMigrate} is not honoured by remote config: baseline then migrate.
     */
    @Bean
    public FlywayMigrationStrategy authenticationFlywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.migrate();
            } catch (FlywayException ex) {
                String message = ex.getMessage() == null ? "" : ex.getMessage();
                if (message.contains("no schema history")) {
                    flyway.baseline();
                    flyway.migrate();
                    return;
                }
                throw ex;
            }
        };
    }
}
