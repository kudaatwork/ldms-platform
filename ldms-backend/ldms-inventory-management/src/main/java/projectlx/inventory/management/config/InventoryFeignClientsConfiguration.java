package projectlx.inventory.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.inventory.management.clients.BillingPaymentsServiceClient;
import projectlx.inventory.management.clients.FileUploadServiceClient;
import projectlx.inventory.management.clients.LocationsServiceClient;
import projectlx.inventory.management.clients.OrganizationServiceClient;
import projectlx.inventory.management.clients.ShipmentManagementServiceClient;
import projectlx.inventory.management.clients.UserManagementServiceClient;

/**
 * Builds Feign clients after Config Server has merged properties so base URLs respect
 * {@code ldms.dev.force-local-feign-clients} and explicit {@code CLIENTS_*} overrides.
 */
@Slf4j
@Configuration
public class InventoryFeignClientsConfiguration {

    @Bean
    public UserManagementServiceClient userManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info("User management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(UserManagementServiceClient.class, "user-management-service")
                .inheritParentContext(true)
                .contextId("inventory-user-management-service")
                .url(url)
                .build();
    }

    @Bean
    public OrganizationServiceClient organizationServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveOrganizationManagementServiceBaseUrl(env);
        log.info("Organization management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(OrganizationServiceClient.class, "organization-management-service")
                .inheritParentContext(true)
                .contextId("inventory-organization-management-service")
                .url(url)
                .build();
    }

    @Bean
    public LocationsServiceClient locationsServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveLocationsManagementServiceBaseUrl(env);
        log.info("Locations management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(LocationsServiceClient.class, "locations-management-service")
                .inheritParentContext(true)
                .contextId("inventory-locations-management-service")
                .url(url)
                .build();
    }

    @Bean
    public FileUploadServiceClient fileUploadServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveFileUploadServiceBaseUrl(env);
        log.info("File upload Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(FileUploadServiceClient.class, "product-file-upload-service")
                .inheritParentContext(true)
                .contextId("inventory-file-upload-service")
                .url(url)
                .build();
    }

    @Bean
    public ShipmentManagementServiceClient shipmentManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveShipmentManagementServiceBaseUrl(env);
        log.info("Shipment management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(ShipmentManagementServiceClient.class, "shipment-management-service")
                .inheritParentContext(true)
                .contextId("inventory-shipment-management-service")
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
                .contextId("inventory-billing-payments-service")
                .url(url)
                .build();
    }
}
