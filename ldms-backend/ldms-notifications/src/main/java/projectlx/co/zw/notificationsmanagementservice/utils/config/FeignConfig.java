package projectlx.co.zw.notificationsmanagementservice.utils.config;

import feign.Logger;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.notificationsmanagementservice.business.logic.api.AuditTrailService;
import projectlx.co.zw.notificationsmanagementservice.utils.audit.CustomFeignLogger;

@Slf4j
@Configuration
public class FeignConfig {

    @Value("${spring.application.name}") // <-- Inject the service name here
    private String serviceName;

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // This is required to log bodies
    }

    /**
     * This is the ONLY bean of the type feign.Logger.
     * We create your CustomFeignLogger here and return it.
     * Spring will automatically provide the AuditTrailService because it's a bean.
     */
    @Bean
    public Logger feignLogger(AuditTrailService auditTrailService) {
        // Now pass the serviceName into the constructor
        log.info("--- FEIGN CONFIG: Creating CustomFeignLogger bean. ---");
        return new CustomFeignLogger(auditTrailService, serviceName);
    }

    /**
     * This bean enables Feign to handle multipart/form-data requests,
     * which is necessary for file uploads.
     */
    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder();
    }
}
