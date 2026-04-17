package projectlx.co.zw.audittrail.business.logic.impl;

import com.rabbitmq.client.Channel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import projectlx.co.zw.audittrail.business.auditable.api.AuditLogServiceAuditable;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogConsumer;
import projectlx.co.zw.audittrail.model.AuditLog;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogPayload;
import projectlx.co.zw.audittrail.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Slf4j
@RequiredArgsConstructor
public class AuditLogConsumerImpl implements AuditLogConsumer {

    private static final Locale LOG_LOCALE = Locale.forLanguageTag(Constants.DEFAULT_LOCALE);

    private final AuditLogServiceAuditable auditLogServiceAuditable;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "ldms.audit.queue", concurrency = "3-10")
    public void consume(AuditLogPayload payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            processInboundAuditLog(payload);
            channel.basicAck(deliveryTag, false);
        } catch (DataIntegrityViolationException ex) {
            log.error(messageService.getMessage(I18Code.AUDIT_CONSUMER_DATA_INTEGRITY.getCode(), new String[] {
                                String.valueOf(payload.getTraceId()),
                                String.valueOf(payload.getServiceName()),
                                String.valueOf(ex.getMessage())
                            },
                            LOG_LOCALE));
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error(messageService.getMessage(I18Code.AUDIT_CONSUMER_INSERT_FAILED.getCode(), new String[] {
                                String.valueOf(payload.getTraceId()),
                                String.valueOf(payload.getServiceName()),
                                String.valueOf(ex.getMessage())
                            },
                            LOG_LOCALE));
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @Override
    public void processInboundAuditLog(AuditLogPayload payload) {
        String actor = Optional.ofNullable(payload.getUsername())
                .filter(s -> !s.isBlank())
                .orElse("SYSTEM");
        auditLogServiceAuditable.create(toEntity(payload), LOG_LOCALE, actor);
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
                .requestHeaders(truncate(serializeHeaders(p.getRequestHeaders()), 65535))
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

    private String serializeHeaders(Object headers) {
        if (headers == null) {
            return null;
        }
        if (headers instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException ex) {
            return String.valueOf(headers);
        }
    }
}
