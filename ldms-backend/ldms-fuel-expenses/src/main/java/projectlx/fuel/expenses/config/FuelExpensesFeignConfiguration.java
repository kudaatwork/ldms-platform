package projectlx.fuel.expenses.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.fuel.expenses.clients.TripTrackingServiceClient;
import projectlx.fuel.expenses.clients.UserManagementServiceClient;

@Slf4j
@Configuration
public class FuelExpensesFeignConfiguration {

    @Bean
    public UserManagementServiceClient userManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info("User management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(UserManagementServiceClient.class, "user-management-service")
                .inheritParentContext(true)
                .contextId("fuel-user-management-service")
                .url(url)
                .build();
    }

    @Bean
    public TripTrackingServiceClient tripTrackingServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveTripTrackingServiceBaseUrl(env);
        log.info("Trip tracking Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(TripTrackingServiceClient.class, "trip-tracking-service")
                .inheritParentContext(true)
                .contextId("fuel-trip-tracking-service")
                .url(url)
                .build();
    }
}
