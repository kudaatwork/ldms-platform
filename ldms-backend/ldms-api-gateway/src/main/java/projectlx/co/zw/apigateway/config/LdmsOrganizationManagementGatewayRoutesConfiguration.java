package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers organization-management routes independently of {@code spring.cloud.gateway.routes} from Config Server.
 * <p>
 * Central config may target {@code lb://organization-management-service} while Eureka registers a different id, or
 * expose a LAN IP while the JVM listens on localhost — both yield HTTP 503. These routes use a direct URI (same
 * pattern as user-management / notifications) and take precedence via a low {@code order} value.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.organization-management.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsOrganizationManagementGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsOrganizationManagementGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${ORGANIZATION_MANAGEMENT_HOST:127.0.0.1}") String organizationManagementHost,
            @Value("${ORGANIZATION_MANAGEMENT_PORT:8087}") int organizationManagementPort) {
        String uri = "http://" + organizationManagementHost + ":" + organizationManagementPort;
        return builder.routes()
                .route(
                        "ldms-organization-management-system",
                        r -> r.order(-100)
                                .path("/ldms-organization-management/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-organization-management-frontend",
                        r -> r.order(-100)
                                .path("/ldms-organization-management/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-organization-management-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-organization-management/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
