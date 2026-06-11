package projectlx.billing.payments.repository.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import projectlx.billing.payments.model.DomainMarkerInterface;
import projectlx.billing.payments.repository.RepositoryMarkerInterface;

@Configuration
@EnableJpaRepositories(basePackageClasses = RepositoryMarkerInterface.class)
@EntityScan(basePackageClasses = DomainMarkerInterface.class)
public class DataConfig {
}
