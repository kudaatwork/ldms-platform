package projectlx.co.zw.shared_library.repository.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import projectlx.co.zw.shared_library.repository.SharedRepositoryMarkerInterface;

/**
 * Shared-library JPA repositories only. Do not {@code @EntityScan} {@code shared_library.model} here — domain
 * {@code @Entity} types belong in owning services (avoids duplicate metadata and keeps shared models as plain Java).
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = {SharedRepositoryMarkerInterface.class})
public class SharedDataConfig {
}
