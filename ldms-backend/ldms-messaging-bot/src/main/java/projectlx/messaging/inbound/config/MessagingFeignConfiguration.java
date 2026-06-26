package projectlx.messaging.inbound.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.messaging.inbound.clients.BillingPaymentsServiceClient;
import projectlx.messaging.inbound.clients.HelpSupportServiceClient;
import projectlx.messaging.inbound.clients.OrganizationManagementServiceClient;
import projectlx.messaging.inbound.clients.UserManagementAgentClient;
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

    @Bean
    public BillingPaymentsServiceClient billingPaymentsServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveBillingPaymentsServiceBaseUrl(env);
        log.info("Billing payments Feign client base URL: {}", url);
        return new FeignClientBuilder(applicationContext)
                .forType(BillingPaymentsServiceClient.class, "billing-payments-service")
                .inheritParentContext(true)
                .contextId("messaging-billing-payments-service")
                .url(url)
                .build();
    }

    @Bean
    public UserManagementAgentClient userManagementAgentClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info("User management agent Feign client base URL: {}", url);
        return new FeignClientBuilder(applicationContext)
                .forType(UserManagementAgentClient.class, "user-management-agent-service")
                .inheritParentContext(true)
                .contextId("messaging-user-management-agent-service")
                .url(url)
                .build();
    }

    @Bean
    public HelpSupportServiceClient helpSupportServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info("Help support Feign client base URL: {}", url);
        return new FeignClientBuilder(applicationContext)
                .forType(HelpSupportServiceClient.class, "help-support-service")
                .inheritParentContext(true)
                .contextId("messaging-help-support-service")
                .url(url)
                .build();
    }
}
