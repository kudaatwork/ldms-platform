package projectlx.co.zw.audittrail.mapper;

import projectlx.co.zw.audittrail.dto.AuditLogDto;
import projectlx.co.zw.audittrail.entity.AuditLog;

public final class AuditLogDtoMapper {

    private AuditLogDtoMapper() {}

    public static AuditLogDto toDto(AuditLog entity, boolean includeLargePayloads) {
        if (entity == null) {
            return null;
        }
        return new AuditLogDto(
                entity.getId(),
                entity.getAction(),
                entity.getClientIpAddress(),
                includeLargePayloads ? entity.getCurlCommand() : null,
                entity.getEventType(),
                entity.getExceptionMessage(),
                entity.getHttpMethod(),
                entity.getHttpStatusCode(),
                entity.getRequestHeaders(),
                includeLargePayloads ? entity.getRequestPayload() : null,
                entity.getRequestUrl(),
                includeLargePayloads ? entity.getResponsePayload() : null,
                entity.getResponseTimeMs(),
                entity.getServiceName(),
                entity.getRequestTimestamp(),
                entity.getResponseTimestamp(),
                entity.getTraceId(),
                entity.getUsername());
    }
}
