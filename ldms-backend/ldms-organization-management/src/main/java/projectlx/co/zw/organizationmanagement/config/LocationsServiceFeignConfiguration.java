package projectlx.co.zw.organizationmanagement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.co.zw.organizationmanagement.clients.LocationsServiceClient;

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
                .forType(LocationsServiceClient.class, "organization-locations-service")
                .inheritParentContext(true)
                .contextId("organization-locations-service")
                .url(url)
                .build();
    }
}
