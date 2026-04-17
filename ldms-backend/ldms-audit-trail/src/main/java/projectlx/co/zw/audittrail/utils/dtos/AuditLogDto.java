package projectlx.co.zw.audittrail.utils.dtos;

import java.time.LocalDateTime;
import projectlx.co.zw.audittrail.model.AuditEventType;

public record AuditLogDto(
        Long id,
        String action,
        String clientIpAddress,
        String curlCommand,
        AuditEventType eventType,
        String exceptionMessage,
        String httpMethod,
        Integer httpStatusCode,
        String requestHeaders,
        String requestPayload,
        String requestUrl,
        String responsePayload,
        Long responseTimeMs,
        String serviceName,
        LocalDateTime requestTimestamp,
        LocalDateTime responseTimestamp,
        String traceId,
        String username) {}
