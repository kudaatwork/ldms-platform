package projectlx.co.zw.audittrail.business.logic.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.audittrail.business.auditable.api.AuditLogServiceAuditable;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogQueryService;
import projectlx.co.zw.audittrail.model.AuditEventType;
import projectlx.co.zw.audittrail.model.AuditLog;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogFilter;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogServiceStats;

public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "requestTimestamp",
            "responseTimestamp",
            "id",
            "serviceName",
            "username",
            "httpStatusCode",
            "eventType",
            "traceId",
            "action");

    private final AuditLogServiceAuditable auditLogServiceAuditable;

    public AuditLogQueryServiceImpl(AuditLogServiceAuditable auditLogServiceAuditable) {
        this.auditLogServiceAuditable = auditLogServiceAuditable;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(AuditLogFilter filter) {

        AuditEventType eventTypeEnum = parseEventType(filter.eventType());

        Pageable pageable = toPageable(filter);

        return auditLogServiceAuditable.search(
                blankToNull(filter.serviceName()),
                blankToNull(filter.username()),
                eventTypeEnum,
                filter.httpStatusCode(),
                filter.from(),
                filter.to(),
                blankToNull(filter.searchValue()),
                blankToNull(filter.action()),
                blankToNull(filter.requestUrl()),
                blankToNull(filter.httpMethod()),
                blankToNull(filter.traceId()),
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLog> findById(Long id) {
        return auditLogServiceAuditable.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByTraceIdOrdered(String traceId) {
        return auditLogServiceAuditable.findByTraceIdOrdered(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogServiceStats buildServiceStats(String serviceName, int hours) {

        LocalDateTime from = LocalDateTime.now().minusHours(hours);

        long total = auditLogServiceAuditable.countInWindow(serviceName, from);
        long httpErrors = auditLogServiceAuditable.countHttpErrorsInWindow(serviceName, from);
        Double avg = auditLogServiceAuditable.avgResponseTimeMsInWindow(serviceName, from);
        double avgMs = avg != null ? avg : 0.0;
        double errorRate = total > 0 ? (httpErrors * 100.0) / total : 0.0;

        Map<String, Long> byEventType = new HashMap<>();

        for (Object[] row : auditLogServiceAuditable.countByEventTypeInWindow(serviceName, from)) {
            AuditEventType et = (AuditEventType) row[0];
            long c = ((Number) row[1]).longValue();
            if (et != null) {
                byEventType.put(et.name(), c);
            }
        }

        Map<Integer, Long> byHttpStatus = new HashMap<>();

        for (Object[] row : auditLogServiceAuditable.countByHttpStatusInWindow(serviceName, from)) {
            Integer code = (Integer) row[0];
            long c = ((Number) row[1]).longValue();
            if (code != null) {
                byHttpStatus.put(code, c);
            }
        }

        return new AuditLogServiceStats(
                serviceName,
                total,
                byEventType.getOrDefault(AuditEventType.EXCEPTION.name(), 0L),
                byEventType.getOrDefault(AuditEventType.FEIGN_CALL.name(), 0L),
                byEventType.getOrDefault(AuditEventType.SERVICE_METHOD.name(), 0L),
                avgMs,
                errorRate,
                Map.copyOf(byEventType),
                Map.copyOf(byHttpStatus));
    }

    private static Pageable toPageable(AuditLogFilter filter) {

        String sortBy = filter.sortBy() == null || filter.sortBy().isBlank()
                ? "requestTimestamp"
                : filter.sortBy().trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "requestTimestamp";
        }

        String dir = filter.sortDir() == null || filter.sortDir().isBlank()
                ? "DESC"
                : filter.sortDir().trim().toUpperCase(Locale.ROOT);
        Sort.Direction direction = "ASC".equals(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(filter.page(), filter.size(), sort);
    }

    private static AuditEventType parseEventType(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }
        return AuditEventType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    private static String blankToNull(String s) {

        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
