package projectlx.co.zw.notificationsmanagementservice.utils.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import projectlx.co.zw.notificationsmanagementservice.business.logic.api.AuditTrailService;
import projectlx.co.zw.notificationsmanagementservice.business.logic.impl.AuditTrailServiceImpl;
import projectlx.co.zw.notificationsmanagementservice.utils.audit.AuditAspect;
import projectlx.co.zw.notificationsmanagementservice.utils.audit.AuditFilter;


@EnableAspectJAutoProxy // <-- HERE
@EnableAsync
@Configuration
public class AuditConfig {

    @Bean
    public AuditTrailService auditTrailService(RabbitTemplate rabbitTemplate) {
        return new AuditTrailServiceImpl(rabbitTemplate);
    }

    @Bean
    public AuditAspect auditAspect(AuditTrailService auditTrailService) {
        return new AuditAspect(auditTrailService);
    }

    @Bean
    public AuditFilter auditFilter(AuditTrailService auditTrailService) {
        return new AuditFilter(auditTrailService);
    }
}
