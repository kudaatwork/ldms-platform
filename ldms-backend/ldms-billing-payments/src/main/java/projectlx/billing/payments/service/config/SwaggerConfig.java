package projectlx.billing.payments.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI billingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LDMS Billing & Payments API")
                        .description("Billing, payments, currency management, and financial transactions")
                        .version("1.0"));
    }
}
