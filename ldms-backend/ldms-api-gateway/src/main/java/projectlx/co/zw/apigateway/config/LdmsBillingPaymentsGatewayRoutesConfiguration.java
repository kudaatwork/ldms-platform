package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ldms.gateway.billing-payments.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsBillingPaymentsGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsBillingPaymentsGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${BILLING_PAYMENTS_HOST:127.0.0.1}") String billingPaymentsHost,
            @Value("${BILLING_PAYMENTS_PORT:8014}") int billingPaymentsPort) {
        String uri = "http://" + billingPaymentsHost + ":" + billingPaymentsPort;
        return builder.routes()
                .route(
                        "ldms-billing-payments-system",
                        r -> r.order(-100)
                                .path("/ldms-billing-payments/v1/system/**")
                                .uri(uri))
                .route(
                        "ldms-billing-payments-frontend",
                        r -> r.order(-100)
                                .path("/ldms-billing-payments/v1/frontend/**")
                                .uri(uri))
                .route(
                        "ldms-billing-payments-backoffice",
                        r -> r.order(-100)
                                .path("/ldms-billing-payments/v1/backoffice/**")
                                .uri(uri))
                .build();
    }
}
