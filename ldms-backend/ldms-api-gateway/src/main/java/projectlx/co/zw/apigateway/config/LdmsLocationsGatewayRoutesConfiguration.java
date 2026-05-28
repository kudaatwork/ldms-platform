package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers location-management routes independently of {@code spring.cloud.gateway.routes} from Config Server.
 * <p>
 * Central config may target {@code lb://ldms-location-management} while Eureka is down or uses a different
 * service id — that yields HTTP 503 even when ldms-locations is running on localhost. These routes use a
 * direct URI (same pattern as organization-management / notifications) and take precedence via {@code order(-100)}.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.locations.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsLocationsGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsLocationsGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${ldms.gateway.locations.host:127.0.0.1}") String locationsHost,
            @Value("${ldms.gateway.locations.port:8084}") int locationsPort) {
        String uri = "http://" + locationsHost + ":" + locationsPort;
        return builder.routes()
                .route(
                        "ldms-locations-system",
                        r -> r.order(-100)
                                .path("/ldms-locations/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-locations-frontend",
                        r -> r.order(-100)
                                .path("/ldms-locations/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-locations-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-locations/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
