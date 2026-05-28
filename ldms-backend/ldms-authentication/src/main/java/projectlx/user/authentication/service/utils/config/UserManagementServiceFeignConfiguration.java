package projectlx.user.authentication.service.utils.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;

@Configuration
public class UserManagementServiceFeignConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UserManagementServiceFeignConfiguration.class);

    @Bean
    public UserManagementServiceClient userManagementServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env);
        log.info(
                "User management Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url,
                env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(UserManagementServiceClient.class, "user-management-service")
                .inheritParentContext(true)
                .contextId("auth-user-management-service")
                .url(url)
                .build();
    }
}
