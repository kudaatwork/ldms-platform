package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Routes {@code ldms-authentication} through the API Gateway (port 8091 by default).
 *
 * <p>Public auth endpoints (no JWT):
 * <ul>
 *   <li>{@code POST /ldms-authentication/v1/auth/request-access-token}</li>
 *   <li>{@code POST /ldms-authentication/v1/auth/refresh-token}</li>
 *   <li>{@code POST /ldms-authentication/v1/auth/google-id-token}</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.authentication.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsAuthenticationGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsAuthenticationGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${ldms.gateway.authentication.host:127.0.0.1}") String authenticationHost,
            @Value("${ldms.gateway.authentication.port:8083}") int authenticationPort) {
        String uri = "http://" + authenticationHost + ":" + authenticationPort;
        return builder.routes()
                .route(
                        "ldms-authentication-programmatic",
                        r -> r.order(-100).path("/ldms-authentication/**").uri(uri))
                .build();
    }
}
