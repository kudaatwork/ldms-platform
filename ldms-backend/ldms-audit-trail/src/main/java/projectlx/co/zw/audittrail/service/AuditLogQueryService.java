package projectlx.co.zw.audittrail.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.audittrail.dto.AuditLogFilter;
import projectlx.co.zw.audittrail.dto.AuditLogServiceStats;
import projectlx.co.zw.audittrail.entity.AuditEventType;
import projectlx.co.zw.audittrail.entity.AuditLog;
import projectlx.co.zw.audittrail.repository.AuditLogRepository;

@Service
public class AuditLogQueryService {

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

    private final AuditLogRepository repository;

    public AuditLogQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(AuditLogFilter filter) {
        AuditEventType eventTypeEnum = parseEventType(filter.eventType());
        Pageable pageable = toPageable(filter);
        return repository.search(
                blankToNull(filter.serviceName()),
                blankToNull(filter.username()),
                eventTypeEnum,
                filter.httpStatusCode(),
                filter.from(),
                filter.to(),
                pageable);
    }

    @Transactional(readOnly = true)
    public Optional<AuditLog> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByTraceIdOrdered(String traceId) {
        return repository.findByTraceIdOrderByRequestTimestampAsc(traceId);
    }

    @Transactional(readOnly = true)
    public AuditLogServiceStats buildServiceStats(String serviceName, int hours) {
        LocalDateTime from = LocalDateTime.now().minusHours(hours);
        long total = repository.countInWindow(serviceName, from);
        long httpErrors = repository.countHttpErrorsInWindow(serviceName, from);
        Double avg = repository.avgResponseTimeMsInWindow(serviceName, from);
        double avgMs = avg != null ? avg : 0.0;
        double errorRate = total > 0 ? (httpErrors * 100.0) / total : 0.0;

        Map<String, Long> byEventType = new HashMap<>();
        for (Object[] row : repository.countByEventTypeInWindow(serviceName, from)) {
            AuditEventType et = (AuditEventType) row[0];
            long c = ((Number) row[1]).longValue();
            if (et != null) {
                byEventType.put(et.name(), c);
            }
        }

        Map<Integer, Long> byHttpStatus = new HashMap<>();
        for (Object[] row : repository.countByHttpStatusInWindow(serviceName, from)) {
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
