package projectlx.co.zw.audittrail.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    AUDIT_LOG_REQUEST_INVALID("audit.log.request.invalid"),
    AUDIT_LOG_SEARCH_SUCCESS("audit.log.search.success"),
    AUDIT_LOG_NOT_FOUND("audit.log.not.found"),
    AUDIT_LOG_RETRIEVED_SUCCESS("audit.log.retrieved.success"),
    AUDIT_LOG_TRACE_RETRIEVED_SUCCESS("audit.log.trace.retrieved.success"),
    AUDIT_LOG_STATS_RETRIEVED_SUCCESS("audit.log.stats.retrieved.success"),
    AUDIT_LOG_CHURN_OUT_SUCCESS("audit.log.churn.out.success"),
    AUDIT_LOG_CHURN_OUT_FAILED("audit.log.churn.out.failed"),
    AUDIT_LOG_CHURN_LAUNCH_ACCEPTED("audit.log.churn.launch.accepted"),
    AUDIT_LOG_CHURN_ALREADY_RUNNING("audit.log.churn.already.running"),

    AUDIT_LOG_VALIDATION_PAGE_INVALID("audit.log.validation.page.invalid"),
    AUDIT_LOG_VALIDATION_SIZE_INVALID("audit.log.validation.size.invalid"),
    AUDIT_LOG_VALIDATION_SORT_BY_INVALID("audit.log.validation.sort.by.invalid"),
    AUDIT_LOG_VALIDATION_SORT_DIR_INVALID("audit.log.validation.sort.dir.invalid"),
    AUDIT_LOG_VALIDATION_EVENT_TYPE_INVALID("audit.log.validation.event.type.invalid"),
    AUDIT_LOG_VALIDATION_ID_INVALID("audit.log.validation.id.invalid"),
    AUDIT_LOG_VALIDATION_TRACE_ID_REQUIRED("audit.log.validation.trace.id.required"),
    AUDIT_LOG_VALIDATION_SERVICE_NAME_REQUIRED("audit.log.validation.service.name.required"),
    AUDIT_LOG_VALIDATION_HOURS_INVALID("audit.log.validation.hours.invalid"),

    AUDIT_CONSUMER_DATA_INTEGRITY("audit.consumer.data.integrity"),
    AUDIT_CONSUMER_INSERT_FAILED("audit.consumer.insert.failed");

    private final String code;
}
