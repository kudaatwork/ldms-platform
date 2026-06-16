package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ldms.gateway.fuel-expenses.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsFuelExpensesGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsFuelExpensesGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${FUEL_EXPENSES_HOST:127.0.0.1}") String fuelExpensesHost,
            @Value("${FUEL_EXPENSES_PORT:8017}") int fuelExpensesPort) {
        String uri = "http://" + fuelExpensesHost + ":" + fuelExpensesPort;
        return builder.routes()
                .route(
                        "ldms-fuel-expenses-frontend",
                        r -> r.order(-100)
                                .path("/ldms-fuel-expenses/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-fuel-expenses-system",
                        r -> r.order(-100)
                                .path("/ldms-fuel-expenses/v1/system/**")
                                .uri(uri))
                .build();
    }
}
