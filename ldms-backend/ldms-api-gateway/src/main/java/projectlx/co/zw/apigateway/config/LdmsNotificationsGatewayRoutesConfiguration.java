package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers notification routes independently of {@code spring.cloud.gateway.routes} from Config Server.
 * <p>
 * Central config currently targets {@code lb://notifications}, but Eureka registers
 * {@code ldms-notifications} — that mismatch yields HTTP 503 even when the service is up.
 * These routes use a direct URI (same pattern as user-management / file-upload) and take
 * precedence via a low {@code order} value.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.notifications.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsNotificationsGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsNotificationsGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${NOTIFICATIONS_HOST:127.0.0.1}") String notificationsHost,
            @Value("${NOTIFICATIONS_PORT:8094}") int notificationsPort) {
        String uri = "http://" + notificationsHost + ":" + notificationsPort;
        return builder.routes()
                .route(
                        "ldms-notifications-system",
                        r -> r.order(-100)
                                .path("/ldms-notifications/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-notifications-frontend",
                        r -> r.order(-100)
                                .path("/ldms-notifications/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-notifications-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-notifications/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
