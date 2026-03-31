package projectlx.user.management.service.utils.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import projectlx.user.management.service.business.logic.api.AuditTrailService;
import projectlx.user.management.service.utils.audit.AuditAspect;
import projectlx.user.management.service.utils.audit.AuditFilter;

@EnableAspectJAutoProxy // <-- HERE
@EnableAsync
@Configuration
public class AuditConfig {

    @Bean
    public AuditAspect auditAspect(AuditTrailService auditTrailService, ObjectMapper objectMapper) {
        return new AuditAspect(auditTrailService, objectMapper);
    }

    @Bean
    public AuditFilter auditFilter(AuditTrailService auditTrailService) {
        return new AuditFilter(auditTrailService);
    }
}
