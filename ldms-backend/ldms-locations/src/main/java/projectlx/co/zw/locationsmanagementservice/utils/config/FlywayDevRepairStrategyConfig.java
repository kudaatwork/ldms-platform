package projectlx.co.zw.locationsmanagementservice.utils.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * When a migration fails once, Flyway records it in {@code flyway_schema_history} and subsequent
 * startups fail validation until {@link Flyway#repair()} is run. For local {@code dev}, repair
 * automatically before migrate so the app can start after fixing migration SQL.
 *
 * <p>Non-{@code dev} profiles keep Spring Boot's default behaviour (migrate only).
 */
@Configuration(proxyBeanMethods = false)
public class FlywayDevRepairStrategyConfig {

    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy devFlywayRepairBeforeMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
