package projectlx.billing.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.security.config.LdmsMethodSecurityConfiguration;
import projectlx.co.zw.shared_library.utils.security.config.SharedJwtSecurityConfig;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class }, scanBasePackages = {
        "projectlx.billing.payments",
        "projectlx.co.zw.shared_library"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableMethodSecurity
@Import({ SharedJwtSecurityConfig.class, LdmsMethodSecurityConfiguration.class, UtilsConfig.class })
public class BillingPaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingPaymentsApplication.class, args);
    }
}
