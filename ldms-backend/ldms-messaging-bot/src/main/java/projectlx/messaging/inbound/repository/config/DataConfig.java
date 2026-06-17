package projectlx.messaging.inbound.repository.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import projectlx.messaging.inbound.model.DomainMarkerInterface;
import projectlx.messaging.inbound.repository.RepositoryMarkerInterface;

@Configuration
@EnableJpaRepositories(basePackageClasses = RepositoryMarkerInterface.class)
@EntityScan(basePackageClasses = DomainMarkerInterface.class)
public class DataConfig {
}
