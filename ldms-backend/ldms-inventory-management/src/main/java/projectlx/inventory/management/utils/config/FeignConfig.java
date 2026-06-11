package projectlx.inventory.management.utils.config;

import feign.Logger;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.inventory.management.business.logic.api.AuditTrailService;
import projectlx.inventory.management.utils.audit.CustomFeignLogger;

@Slf4j
@Configuration
public class FeignConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    private final ObjectFactory<HttpMessageConverters> messageConverters;

    public FeignConfig(ObjectFactory<HttpMessageConverters> messageConverters) {
        this.messageConverters = messageConverters;
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Logger feignLogger(AuditTrailService auditTrailService) {
        log.info("--- FEIGN CONFIG: Creating CustomFeignLogger bean. ---");
        return new CustomFeignLogger(auditTrailService, serviceName);
    }

    @Bean
    public Encoder feignEncoder() {
        // This encoder combines the SpringFormEncoder for multipart/form-data
        // with the SpringEncoder for standard application/json requests.
        return new SpringFormEncoder(new SpringEncoder(this.messageConverters));
    }
}