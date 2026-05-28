package projectlx.co.zw.locationsmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.security.config.LdmsMethodSecurityConfiguration;
import projectlx.co.zw.shared_library.utils.security.config.SharedJwtSecurityConfig;

@EnableFeignClients
@SpringBootApplication(
        exclude = { UserDetailsServiceAutoConfiguration.class },
        scanBasePackages = {
                "projectlx.co.zw.locationsmanagementservice",
                "projectlx.co.zw.shared_library"
        })
@EnableMethodSecurity
@Import({ SharedJwtSecurityConfig.class, LdmsMethodSecurityConfiguration.class, UtilsConfig.class })
public class LocationsManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationsManagementServiceApplication.class, args);
    }

}
