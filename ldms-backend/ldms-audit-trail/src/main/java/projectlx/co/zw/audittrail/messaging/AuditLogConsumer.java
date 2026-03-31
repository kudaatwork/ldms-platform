package projectlx.co.zw.audittrail.messaging;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.entity.AuditLog;
import projectlx.co.zw.audittrail.repository.AuditLogRepository;

@Component
@Slf4j
public class AuditLogConsumer {

    private final AuditLogRepository repository;

    public AuditLogConsumer(AuditLogRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "ldms.audit.queue", concurrency = "3-10")
    public void consume(
            AuditLogPayload payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            AuditLog entity = toEntity(payload);
            repository.save(entity);
            channel.basicAck(deliveryTag, false);
        } catch (DataIntegrityViolationException ex) {
            log.error(
                    "[AuditConsumer] Data integrity violation (ack, no DLQ) trace={} service={}: {}",
                    payload.getTraceId(),
                    payload.getServiceName(),
                    ex.getMessage());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error(
                    "[AuditConsumer] Insert failed for trace={} service={}: {}",
                    payload.getTraceId(),
                    payload.getServiceName(),
                    ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private AuditLog toEntity(AuditLogPayload p) {
        return AuditLog.builder()
                .traceId(p.getTraceId())
                .serviceName(p.getServiceName())
                .username(p.getUsername())
                .clientIpAddress(p.getClientIpAddress())
                .action(p.getAction())
                .eventType(p.getEventType())
                .httpMethod(p.getHttpMethod())
                .requestUrl(truncate(p.getRequestUrl(), 255))
                .requestHeaders(p.getRequestHeaders())
                .requestPayload(p.getRequestPayload())
                .responsePayload(p.getResponsePayload())
                .httpStatusCode(p.getHttpStatusCode())
                .responseTimeMs(p.getResponseTimeMs())
                .curlCommand(p.getCurlCommand())
                .exceptionMessage(p.getExceptionMessage())
                .requestTimestamp(p.getRequestTimestamp())
                .responseTimestamp(p.getResponseTimestamp())
                .build();
    }

    private String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }
}
