package projectlx.co.zw.audittrail.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogService;
import projectlx.co.zw.audittrail.service.processor.api.AuditLogProcessor;
import projectlx.co.zw.audittrail.service.processor.impl.AuditLogProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public AuditLogProcessor auditLogProcessor(AuditLogService auditLogService) {
        return new AuditLogProcessorImpl(auditLogService);
    }
}
