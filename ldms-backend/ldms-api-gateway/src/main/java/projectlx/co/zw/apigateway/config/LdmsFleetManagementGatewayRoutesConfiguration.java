package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers fleet-management routes independently of {@code spring.cloud.gateway.routes} from Config Server.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.fleet-management.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsFleetManagementGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsFleetManagementGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${FLEET_MANAGEMENT_HOST:127.0.0.1}") String fleetManagementHost,
            @Value("${FLEET_MANAGEMENT_PORT:8089}") int fleetManagementPort) {
        String uri = "http://" + fleetManagementHost + ":" + fleetManagementPort;
        return builder.routes()
                .route(
                        "ldms-fleet-management-system",
                        r -> r.order(-100)
                                .path("/ldms-fleet-management/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-fleet-management-frontend",
                        r -> r.order(-100)
                                .path("/ldms-fleet-management/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-fleet-management-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-fleet-management/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
