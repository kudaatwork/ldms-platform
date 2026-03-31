package projectlx.co.zw.notifications.repository.config;


import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import projectlx.co.zw.notifications.model.DomainMarkerInterface;
import projectlx.co.zw.notifications.repository.RepositoryMarkerInterface;

@Configuration
@EnableJpaRepositories(basePackageClasses = {RepositoryMarkerInterface.class})
@EntityScan(basePackageClasses = {DomainMarkerInterface.class})
public class DataConfig {
}
