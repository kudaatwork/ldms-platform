package projectlx.co.zw.organizationmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import projectlx.co.zw.organizationmanagement.utils.config.UserTypesProperties;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.security.config.SharedJwtSecurityConfig;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class }, scanBasePackages = {
        "projectlx.co.zw.organizationmanagement",
        "projectlx.co.zw.shared_library"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "projectlx.co.zw.organizationmanagement.clients")
@EnableMethodSecurity
@EnableConfigurationProperties(UserTypesProperties.class)
@Import({ SharedJwtSecurityConfig.class, UtilsConfig.class })
public class OrganizationManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationManagementApplication.class, args);
    }
}
