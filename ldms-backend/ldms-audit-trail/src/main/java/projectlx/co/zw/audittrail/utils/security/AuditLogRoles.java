package projectlx.co.zw.audittrail.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AuditLogRoles {

    SEARCH_AUDIT_LOGS("SEARCH_AUDIT_LOGS", "Search audit logs with filters"),
    VIEW_AUDIT_LOG_BY_ID("VIEW_AUDIT_LOG_BY_ID", "View a single audit log by id"),
    VIEW_AUDIT_LOGS_BY_TRACE("VIEW_AUDIT_LOGS_BY_TRACE", "View audit logs for a trace id"),
    VIEW_AUDIT_LOG_SERVICE_STATS("VIEW_AUDIT_LOG_SERVICE_STATS", "View aggregated audit stats for a service"),
    CHURN_OUT_AUDIT_LOGS("CHURN_OUT_AUDIT_LOGS", "Purge request logs and create churn history entries");

    private final String roleName;
    private final String description;
}
