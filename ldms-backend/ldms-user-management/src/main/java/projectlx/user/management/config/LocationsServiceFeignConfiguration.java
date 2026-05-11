package projectlx.user.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import projectlx.user.management.clients.LocationsServiceClient;

/**
 * Builds {@link LocationsServiceClient} after Config Server has merged properties, so the Feign base URL
 * respects {@code ldms.dev.force-local-feign-clients} and {@code CLIENTS_LOCATION_SERVICE_URL}.
 */
@Slf4j
@Configuration
public class LocationsServiceFeignConfiguration {

    @Bean
    public LocationsServiceClient locationsServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveLocationServiceBaseUrl(env);
        log.info("Locations Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(LocationsServiceClient.class, "locations-management-service")
                .inheritParentContext(true)
                .contextId("locations-management-service")
                .url(url)
                .build();
    }
}
