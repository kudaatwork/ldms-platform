package projectlx.co.zw.audittrail.utils.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import projectlx.co.zw.audittrail.business.logic.api.AuditTrailService;
import projectlx.co.zw.audittrail.utils.audit.AuditAspect;
import projectlx.co.zw.audittrail.utils.audit.AuditFilter;

@EnableAspectJAutoProxy
@EnableAsync
@Configuration
public class AuditConfig {

    @Bean
    public AuditAspect auditAspect(AuditTrailService auditTrailService) {
        return new AuditAspect(auditTrailService);
    }

    @Bean
    public AuditFilter auditFilter(AuditTrailService auditTrailService) {
        return new AuditFilter(auditTrailService);
    }
}
