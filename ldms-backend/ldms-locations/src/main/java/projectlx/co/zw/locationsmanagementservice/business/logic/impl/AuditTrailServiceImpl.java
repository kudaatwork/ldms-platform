package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AuditTrailService;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;

@Slf4j
@RequiredArgsConstructor
public class AuditTrailServiceImpl implements AuditTrailService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${audit.rabbitmq.exchange}")
    private String exchange;

    @Value("${audit.rabbitmq.routingkey}")
    private String routingKey;

    @Async // Fire-and-forget: send an audit log without blocking the main thread
    @Override
    public void sendAuditLog(AuditLogDto auditLogDto) {
        try {
            log.info("Sending audit log for action: {}", auditLogDto.getAction());
            rabbitTemplate.convertAndSend(exchange, routingKey, auditLogDto);
        } catch (Exception e) {
            log.error("Failed to send audit log to queue. Error: {}", e.getMessage());
        }
    }
}
