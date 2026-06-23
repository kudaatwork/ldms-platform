package projectlx.co.zw.notifications.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.co.zw.notifications.clients.BillingPaymentsServiceClient;

@Slf4j
@Configuration
public class BillingPaymentsServiceFeignConfiguration {

    @Bean
    public BillingPaymentsServiceClient billingPaymentsServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveBillingPaymentsServiceBaseUrl(env);
        log.info("Billing payments Feign client base URL: {}", url);
        return new FeignClientBuilder(applicationContext)
                .forType(BillingPaymentsServiceClient.class, "billing-payments-service")
                .inheritParentContext(true)
                .contextId("notifications-billing-payments-service")
                .url(url)
                .build();
    }
}
