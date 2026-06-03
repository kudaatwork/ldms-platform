package projectlx.co.zw.organizationmanagement.business.logic.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import projectlx.co.zw.organizationmanagement.business.logic.api.AuditTrailService;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;

@Service
public class AuditTrailServiceImpl implements AuditTrailService {

    private final RabbitTemplate rabbitTemplate;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditTrailServiceImpl.class);

    public AuditTrailServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Value("${audit.rabbitmq.exchange:${ldms.audit-rabbitmq.exchange:ldms.audit.exchange}}")
    private String exchange;

    @Value("${audit.rabbitmq.routingkey:${ldms.audit-rabbitmq.routing-key:audit.log}}")
    private String routingKey;

    @Async
    @Override
    public void sendAuditLog(AuditLogDto auditLogDto) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending audit log for action: {}", auditLogDto.getAction());
            }
            rabbitTemplate.convertAndSend(exchange, routingKey, auditLogDto);
        } catch (Exception e) {
            log.error("Failed to send audit log to queue. Error: {}", e.getMessage());
        }
    }
}
