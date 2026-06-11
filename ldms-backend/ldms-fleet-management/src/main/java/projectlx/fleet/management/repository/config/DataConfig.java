package projectlx.fleet.management.repository.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import projectlx.fleet.management.model.DomainMarkerInterface;
import projectlx.fleet.management.repository.RepositoryMarkerInterface;

@Configuration
@EnableJpaRepositories(basePackageClasses = RepositoryMarkerInterface.class)
@EntityScan(basePackageClasses = DomainMarkerInterface.class)
public class DataConfig {}
