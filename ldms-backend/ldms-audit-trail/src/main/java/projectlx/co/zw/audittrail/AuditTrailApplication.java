package projectlx.co.zw.audittrail;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import projectlx.co.zw.audittrail.config.LdmsAuditProperties;

@SpringBootApplication
@EnableRabbit
@EnableConfigurationProperties(LdmsAuditProperties.class)
public class AuditTrailApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditTrailApplication.class, args);
    }
}
