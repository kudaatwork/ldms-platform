package projectlx.trip.tracking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.trip.tracking.clients.InventoryManagementServiceClient;
import projectlx.trip.tracking.clients.ShipmentManagementServiceClient;
import projectlx.trip.tracking.clients.UserManagementServiceClient;

@Slf4j
@Configuration
public class TripTrackingFeignConfiguration {

    @Bean
    public UserManagementServiceClient userManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info("User management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(UserManagementServiceClient.class, "user-management-service")
                .inheritParentContext(true)
                .contextId("trip-user-management-service")
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
                .contextId("trip-shipment-management-service")
                .url(url)
                .build();
    }

    @Bean
    public InventoryManagementServiceClient inventoryManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveInventoryManagementServiceBaseUrl(env);
        log.info("Inventory management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(InventoryManagementServiceClient.class, "inventory-management-service")
                .inheritParentContext(true)
                .contextId("trip-inventory-management-service")
                .url(url)
                .build();
    }
}
