package projectlx.co.zw.shared_library.repository.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import projectlx.co.zw.shared_library.model.SharedDomainMarkerInterface;
import projectlx.co.zw.shared_library.repository.SharedRepositoryMarkerInterface;

@Configuration
@EnableJpaRepositories(basePackageClasses = {SharedRepositoryMarkerInterface.class})
@EntityScan(basePackageClasses = {SharedDomainMarkerInterface.class})
public class SharedDataConfig {
}
