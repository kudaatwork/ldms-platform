package projectlx.co.zw.audittrail.utils.dtos;

import java.util.Map;

public record AuditLogServiceStats(
        String serviceName,
        Long totalRequests,
        Long totalExceptions,
        Long totalFeignCalls,
        Long totalServiceMethods,
        Double avgResponseTimeMs,
        Double errorRate,
        Map<String, Long> byEventType,
        Map<Integer, Long> byHttpStatus) {}
