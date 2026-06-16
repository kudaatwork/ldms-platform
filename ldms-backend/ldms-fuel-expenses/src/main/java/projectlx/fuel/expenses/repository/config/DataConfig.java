package projectlx.fuel.expenses.repository.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import projectlx.fuel.expenses.model.DomainMarkerInterface;
import projectlx.fuel.expenses.repository.RepositoryMarkerInterface;

@Configuration
@EnableJpaRepositories(basePackageClasses = RepositoryMarkerInterface.class)
@EntityScan(basePackageClasses = DomainMarkerInterface.class)
public class DataConfig {
}
