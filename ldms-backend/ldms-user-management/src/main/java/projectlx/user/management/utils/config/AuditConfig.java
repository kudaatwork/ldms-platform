package projectlx.user.management.utils.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import projectlx.user.management.business.logic.api.AuditTrailService;
import projectlx.user.management.utils.audit.AuditAspect;
import projectlx.user.management.utils.audit.AuditFilter;

@EnableAspectJAutoProxy // <-- HERE
@EnableAsync
@Configuration
public class AuditConfig {

    /** Dedicated pool so audit work does not block request threads while the HTTP response is being completed. */
    @Bean(name = "ldmsAuditDispatchExecutor")
    public TaskExecutor ldmsAuditDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("ldms-audit-dispatch-");
        executor.initialize();
        return executor;
    }

    @Bean
    public AuditAspect auditAspect(
            AuditTrailService auditTrailService,
            ObjectMapper objectMapper,
            @Qualifier("ldmsAuditDispatchExecutor") TaskExecutor ldmsAuditDispatchExecutor) {
        return new AuditAspect(auditTrailService, objectMapper, ldmsAuditDispatchExecutor);
    }

    @Bean
    public AuditFilter auditFilter(AuditTrailService auditTrailService) {
        return new AuditFilter(auditTrailService);
    }
}
