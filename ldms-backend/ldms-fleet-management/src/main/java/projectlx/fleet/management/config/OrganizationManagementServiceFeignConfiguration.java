package projectlx.fleet.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.fleet.management.clients.OrganizationManagementServiceClient;

@Slf4j
@Configuration
public class OrganizationManagementServiceFeignConfiguration {

    @Bean
    public OrganizationManagementServiceClient organizationManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveOrganizationManagementServiceBaseUrl(env);
        log.info("Organization management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(OrganizationManagementServiceClient.class, "organization-management-service")
                .inheritParentContext(true)
                .contextId("fleet-organization-management-service")
                .url(url)
                .build();
    }
}
