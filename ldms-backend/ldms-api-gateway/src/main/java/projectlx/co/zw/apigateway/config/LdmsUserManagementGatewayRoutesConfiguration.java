package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Direct user-management routes (same pattern as organisation-management). Avoids 404/503 when
 * Config Server targets the wrong Eureka service id or host.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.user-management.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsUserManagementGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsUserManagementGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${USER_MANAGEMENT_HOST:127.0.0.1}") String userManagementHost,
            @Value("${USER_MANAGEMENT_PORT:8086}") int userManagementPort) {
        String uri = "http://" + userManagementHost + ":" + userManagementPort;
        return builder.routes()
                .route(
                        "ldms-user-management-system-programmatic",
                        r -> r.order(-100)
                                .path("/ldms-user-management/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-user-management-frontend-programmatic",
                        r -> r.order(-100)
                                .path("/ldms-user-management/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-user-management-backoffice-programmatic",
                        r -> r.order(-100)
                                .path("/ldms-user-management/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
