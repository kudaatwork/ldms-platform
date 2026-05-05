package projectlx.co.zw.audittrail.business.auditable.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.audittrail.business.auditable.api.AuditLogServiceAuditable;
import projectlx.co.zw.audittrail.model.AuditEventType;
import projectlx.co.zw.audittrail.model.AuditLog;
import projectlx.co.zw.audittrail.repository.AuditLogRepository;

@RequiredArgsConstructor
public class AuditLogServiceAuditableImpl implements AuditLogServiceAuditable {

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLog create(AuditLog auditLog, Locale locale, String username) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    public AuditLog update(AuditLog auditLog, Locale locale, String username) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    public AuditLog delete(AuditLog auditLog, Locale locale, String username) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(
            String serviceName,
            String username,
            AuditEventType eventType,
            Integer httpStatusCode,
            LocalDateTime from,
            LocalDateTime to,
            String searchValue,
            String action,
            String requestUrl,
            String httpMethod,
            String traceId,
            Pageable pageable) {
        return auditLogRepository.search(
                serviceName,
                username,
                eventType,
                httpStatusCode,
                from,
                to,
                searchValue,
                action,
                requestUrl,
                httpMethod,
                traceId,
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLog> findById(Long id) {
        return auditLogRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByTraceIdOrdered(String traceId) {
        return auditLogRepository.findByTraceIdOrderByRequestTimestampAsc(traceId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countInWindow(String serviceName, LocalDateTime from) {
        return auditLogRepository.countInWindow(serviceName, from);
    }

    @Override
    @Transactional(readOnly = true)
    public long countHttpErrorsInWindow(String serviceName, LocalDateTime from) {
        return auditLogRepository.countHttpErrorsInWindow(serviceName, from);
    }

    @Override
    @Transactional(readOnly = true)
    public Double avgResponseTimeMsInWindow(String serviceName, LocalDateTime from) {
        return auditLogRepository.avgResponseTimeMsInWindow(serviceName, from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countByEventTypeInWindow(String serviceName, LocalDateTime from) {
        return auditLogRepository.countByEventTypeInWindow(serviceName, from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countByHttpStatusInWindow(String serviceName, LocalDateTime from) {
        return auditLogRepository.countByHttpStatusInWindow(serviceName, from);
    }
}
