package projectlx.shipment.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.shipment.management.clients.BillingPaymentsServiceClient;
import projectlx.shipment.management.clients.FleetManagementServiceClient;
import projectlx.shipment.management.clients.OrganizationManagementServiceClient;
import projectlx.shipment.management.clients.TripTrackingServiceClient;
import projectlx.shipment.management.clients.UserManagementServiceClient;

@Slf4j
@Configuration
public class ShipmentFeignConfiguration {

    @Bean
    public UserManagementServiceClient userManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info("User management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(UserManagementServiceClient.class, "user-management-service")
                .inheritParentContext(true)
                .contextId("shipment-user-management-service")
                .url(url)
                .build();
    }

    @Bean
    public OrganizationManagementServiceClient organizationManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveOrganizationManagementServiceBaseUrl(env);
        log.info("Organization management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(OrganizationManagementServiceClient.class, "organization-management-service")
                .inheritParentContext(true)
                .contextId("shipment-organization-management-service")
                .url(url)
                .build();
    }

    @Bean
    public FleetManagementServiceClient fleetManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveFleetManagementServiceBaseUrl(env);
        log.info("Fleet management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(FleetManagementServiceClient.class, "fleet-management-service")
                .inheritParentContext(true)
                .contextId("shipment-fleet-management-service")
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
                .contextId("shipment-trip-tracking-service")
                .url(url)
                .build();
    }

    @Bean
    public BillingPaymentsServiceClient billingPaymentsServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveBillingPaymentsServiceBaseUrl(env);
        log.info("Billing payments Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(BillingPaymentsServiceClient.class, "billing-payments-service")
                .inheritParentContext(true)
                .contextId("shipment-billing-payments-service")
                .url(url)
                .build();
    }
}
