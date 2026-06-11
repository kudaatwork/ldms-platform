package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers inventory-management routes independently of {@code spring.cloud.gateway.routes} from Config Server.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.inventory-management.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsInventoryManagementGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsInventoryManagementGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${INVENTORY_MANAGEMENT_HOST:127.0.0.1}") String inventoryManagementHost,
            @Value("${INVENTORY_MANAGEMENT_PORT:8013}") int inventoryManagementPort) {
        String uri = "http://" + inventoryManagementHost + ":" + inventoryManagementPort;
        return builder.routes()
                .route(
                        "ldms-inventory-management-system",
                        r -> r.order(-100)
                                .path("/ldms-inventory-management/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-inventory-management-frontend",
                        r -> r.order(-100)
                                .path("/ldms-inventory-management/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-inventory-management-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-inventory-management/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
