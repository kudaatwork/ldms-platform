package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ldms.gateway.shipment-management.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsShipmentManagementGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsShipmentManagementGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${SHIPMENT_MANAGEMENT_HOST:127.0.0.1}") String shipmentManagementHost,
            @Value("${SHIPMENT_MANAGEMENT_PORT:8015}") int shipmentManagementPort) {
        String uri = "http://" + shipmentManagementHost + ":" + shipmentManagementPort;
        return builder.routes()
                .route(
                        "ldms-shipment-management-system",
                        r -> r.order(-100)
                                .path("/ldms-shipment-management/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-shipment-management-frontend",
                        r -> r.order(-100)
                                .path("/ldms-shipment-management/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-shipment-management-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-shipment-management/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
