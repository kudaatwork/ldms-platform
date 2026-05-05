package projectlx.co.zw.audittrail.business.validation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import projectlx.co.zw.audittrail.business.validation.api.AuditLogQueryValidator;
import projectlx.co.zw.audittrail.model.AuditEventType;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogFilter;
import projectlx.co.zw.audittrail.utils.enums.I18Code;
import projectlx.co.zw.audittrail.utils.requests.AuditLogMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@RequiredArgsConstructor
public class AuditLogQueryValidatorImpl implements AuditLogQueryValidator {

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

    private final MessageService messageService;

    @Override
    public ValidatorDto validateMultipleFiltersRequest(AuditLogMultipleFiltersRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();
        if (request.getPage() < 0) {
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_PAGE_INVALID.getCode(), new String[] {}, locale));
        }
        return errors.isEmpty()
                ? new ValidatorDto(true, null, List.of())
                : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateSearch(AuditLogFilter filter, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (filter.page() < 0) {
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_PAGE_INVALID.getCode(), new String[] {}, locale));
        }
        if (filter.size() < 1 || filter.size() > 100) {
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_SIZE_INVALID.getCode(), new String[] {}, locale));
        }
        String sortBy = filter.sortBy() == null || filter.sortBy().isBlank()
                ? "requestTimestamp"
                : filter.sortBy().trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            String allowed = ALLOWED_SORT_FIELDS.stream().sorted().collect(Collectors.joining(", "));
            errors.add(messageService.getMessage(
                    I18Code.AUDIT_LOG_VALIDATION_SORT_BY_INVALID.getCode(), new String[] {allowed}, locale));
        }
        String dir = filter.sortDir() == null || filter.sortDir().isBlank()
                ? "DESC"
                : filter.sortDir().trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(dir) && !"DESC".equals(dir)) {
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_SORT_DIR_INVALID.getCode(), new String[] {}, locale));
        }
        if (filter.eventType() != null && !filter.eventType().isBlank()) {
            try {
                AuditEventType.valueOf(filter.eventType().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_EVENT_TYPE_INVALID.getCode(), new String[] {}, locale));
            }
        }
        return errors.isEmpty()
                ? new ValidatorDto(true, null, List.of())
                : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateId(Long id, Locale locale) {
        if (id == null || id < 1) {
            return new ValidatorDto(
                    false,
                    null,
                    List.of(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_ID_INVALID.getCode(), new String[] {}, locale)));
        }
        return new ValidatorDto(true, null, List.of());
    }

    @Override
    public ValidatorDto validateTraceId(String traceId, Locale locale) {
        if (traceId == null || traceId.isBlank()) {
            return new ValidatorDto(
                    false,
                    null,
                    List.of(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_TRACE_ID_REQUIRED.getCode(), new String[] {}, locale)));
        }
        return new ValidatorDto(true, null, List.of());
    }

    @Override
    public ValidatorDto validateServiceStats(String serviceName, int hours, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (serviceName == null || serviceName.isBlank()) {
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_SERVICE_NAME_REQUIRED.getCode(), new String[] {}, locale));
        }
        if (hours < 1 || hours > 8760) {
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_VALIDATION_HOURS_INVALID.getCode(), new String[] {}, locale));
        }
        return errors.isEmpty()
                ? new ValidatorDto(true, null, List.of())
                : new ValidatorDto(false, null, errors);
    }
}
