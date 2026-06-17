package projectlx.messaging.inbound.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.messaging.inbound.clients.OrganizationManagementServiceClient;
import projectlx.messaging.inbound.clients.UserManagementServiceClient;

@Slf4j
@Configuration
public class MessagingFeignConfiguration {

    @Bean
    public UserManagementServiceClient userManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info("User management Feign client base URL: {}", url);
        return new FeignClientBuilder(applicationContext)
                .forType(UserManagementServiceClient.class, "user-management-service")
                .inheritParentContext(true)
                .contextId("messaging-user-management-service")
                .url(url)
                .build();
    }

    @Bean
    public OrganizationManagementServiceClient organizationManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveOrganizationManagementServiceBaseUrl(env);
        log.info("Organization management Feign client base URL: {}", url);
        return new FeignClientBuilder(applicationContext)
                .forType(OrganizationManagementServiceClient.class, "organization-management-service")
                .inheritParentContext(true)
                .contextId("messaging-organization-management-service")
                .url(url)
                .build();
    }
}
