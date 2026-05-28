package projectlx.co.zw.audittrail;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import projectlx.co.zw.audittrail.utils.config.LdmsAuditProperties;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.security.config.LdmsMethodSecurityConfiguration;
import projectlx.co.zw.shared_library.utils.security.config.SharedJwtSecurityConfig;

@SpringBootApplication(
        exclude = { UserDetailsServiceAutoConfiguration.class },
        scanBasePackages = {
                "projectlx.co.zw.audittrail",
                "projectlx.co.zw.shared_library"
        })
@EnableRabbit
@EnableScheduling
@EnableMethodSecurity
@EnableConfigurationProperties(LdmsAuditProperties.class)
@Import({ SharedJwtSecurityConfig.class, LdmsMethodSecurityConfiguration.class, UtilsConfig.class })
public class AuditTrailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditTrailServiceApplication.class, args);
    }
}
