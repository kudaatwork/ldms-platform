package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Direct audit-trail routes (same pattern as notifications / user-management). Avoids 503 when
 * Config Server or Eureka targets {@code lb://audit-trail} instead of {@code ldms-audit-trail},
 * or when the service is running locally without registry lookup.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.audit-trail.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsAuditTrailGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsAuditTrailGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${ldms.gateway.audit-trail.host:127.0.0.1}") String auditTrailHost,
            @Value("${ldms.gateway.audit-trail.port:8099}") int auditTrailPort) {
        String uri = "http://" + auditTrailHost + ":" + auditTrailPort;
        return builder.routes()
                .route(
                        "ldms-audit-trail-programmatic",
                        r -> r.order(-100).path("/ldms-audit-trail/**").uri(uri))
                .build();
    }
}
