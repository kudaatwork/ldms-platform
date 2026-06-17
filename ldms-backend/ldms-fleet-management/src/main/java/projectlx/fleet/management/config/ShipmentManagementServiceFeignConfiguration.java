package projectlx.fleet.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.fleet.management.clients.ShipmentManagementServiceClient;

@Configuration
@Slf4j
public class ShipmentManagementServiceFeignConfiguration {

    @Bean
    public ShipmentManagementServiceClient shipmentManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveShipmentManagementServiceBaseUrl(env);
        log.info("Shipment management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(ShipmentManagementServiceClient.class, "shipment-management-service")
                .url(url)
                .build();
    }
}
