package projectlx.co.zw.audittrail.business.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.audittrail.business.auditable.api.AuditLogServiceAuditable;
import projectlx.co.zw.audittrail.business.auditable.impl.AuditLogServiceAuditableImpl;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogQueryService;
import projectlx.co.zw.audittrail.business.logic.impl.AuditLogConsumerImpl;
import projectlx.co.zw.audittrail.business.logic.impl.AuditLogQueryServiceImpl;
import projectlx.co.zw.audittrail.business.validation.api.AuditLogQueryValidator;
import projectlx.co.zw.audittrail.business.validation.impl.AuditLogQueryValidatorImpl;
import projectlx.co.zw.audittrail.repository.AuditLogRepository;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Configuration
public class BusinessConfig {

    @Bean
    public AuditLogQueryValidator auditLogQueryValidator(MessageService messageService) {
        return new AuditLogQueryValidatorImpl(messageService);
    }

    @Bean
    public AuditLogServiceAuditable auditLogServiceAuditable(AuditLogRepository auditLogRepository) {
        return new AuditLogServiceAuditableImpl(auditLogRepository);
    }

    @Bean
    public AuditLogQueryService auditLogQueryService(AuditLogServiceAuditable auditLogServiceAuditable) {
        return new AuditLogQueryServiceImpl(auditLogServiceAuditable);
    }

    @Bean
    public AuditLogConsumerImpl auditLogConsumer(
            AuditLogServiceAuditable auditLogServiceAuditable,
            MessageService messageService,
            ObjectMapper objectMapper) {
        return new AuditLogConsumerImpl(auditLogServiceAuditable, messageService, objectMapper);
    }
}
