package projectlx.co.zw.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs key gateway routes at startup so local dev can confirm authentication and other paths are registered.
 */
@Component
public class GatewayStartupRoutesLogger {

    private static final Logger log = LoggerFactory.getLogger(GatewayStartupRoutesLogger.class);

    private final RouteLocator routeLocator;
    private final int authenticationPort;

    public GatewayStartupRoutesLogger(
            RouteLocator routeLocator,
            @Value("${ldms.gateway.authentication.port:8083}") int authenticationPort) {
        this.routeLocator = routeLocator;
        this.authenticationPort = authenticationPort;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logKeyRoutes() {
        routeLocator.getRoutes()
                .filter(route -> route.getId() != null && isLoggedRoute(route.getId()))
                .subscribe(route -> log.info("LDMS API Gateway route [{}] -> {}", route.getId(), route.getUri()));
        log.info(
                "Login URL: POST http://localhost:8091/ldms-authentication/v1/auth/request-access-token "
                        + "(requires ldms-authentication on :" + authenticationPort + ")");
        log.info(
                "Platform health: GET http://localhost:8091/ldms-api-gateway/v1/backoffice/platform-health/snapshot");
    }

    private static boolean isLoggedRoute(String routeId) {
        return routeId.contains("authentication")
                || routeId.contains("locations")
                || routeId.contains("notifications")
                || routeId.contains("audit-trail")
                || routeId.contains("organization-management");
    }
}
