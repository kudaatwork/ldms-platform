package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ldms.gateway.trip-tracking.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsTripTrackingGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsTripTrackingGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${TRIP_TRACKING_HOST:127.0.0.1}") String tripTrackingHost,
            @Value("${TRIP_TRACKING_PORT:8016}") int tripTrackingPort) {
        String uri = "http://" + tripTrackingHost + ":" + tripTrackingPort;
        return builder.routes()
                .route(
                        "ldms-trip-tracking-system",
                        r -> r.order(-100)
                                .path("/ldms-trip-tracking/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-trip-tracking-frontend",
                        r -> r.order(-100)
                                .path("/ldms-trip-tracking/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-trip-tracking-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-trip-tracking/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
