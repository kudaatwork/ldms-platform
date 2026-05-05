package projectlx.co.zw.audittrail;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import projectlx.co.zw.audittrail.utils.config.LdmsAuditProperties;

@SpringBootApplication(scanBasePackages = {
        "projectlx.co.zw.audittrail",
        "projectlx.co.zw.shared_library"
})
@EnableRabbit
@EnableScheduling
@EnableMethodSecurity
@EnableConfigurationProperties(LdmsAuditProperties.class)
public class AuditTrailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditTrailServiceApplication.class, args);
    }
}
