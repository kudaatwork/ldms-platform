package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ldms.gateway.messaging-inbound.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsMessagingInboundGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsMessagingInboundGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${MESSAGING_INBOUND_HOST:127.0.0.1}") String host,
            @Value("${MESSAGING_INBOUND_PORT:8095}") int port) {
        String uri = "http://" + host + ":" + port;
        return builder.routes()
                .route(
                        "ldms-messaging-inbound-system",
                        r -> r.order(-100)
                                .path("/ldms-messaging-inbound/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-messaging-inbound-frontend",
                        r -> r.order(-100)
                                .path("/ldms-messaging-inbound/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-messaging-inbound-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-messaging-inbound/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
