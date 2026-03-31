package projectlx.co.zw.audittrail.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.dto.AuditLogFilter;
import projectlx.co.zw.audittrail.entity.AuditEventType;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

@Component
public class AuditLogQueryValidator {

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

    public ValidatorDto validateSearch(AuditLogFilter filter) {
        List<String> errors = new ArrayList<>();
        if (filter.page() < 0) {
            errors.add("page must be >= 0");
        }
        if (filter.size() < 1 || filter.size() > 100) {
            errors.add("size must be between 1 and 100");
        }
        String sortBy = filter.sortBy() == null || filter.sortBy().isBlank()
                ? "requestTimestamp"
                : filter.sortBy().trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            errors.add("sortBy must be one of: " + String.join(", ", ALLOWED_SORT_FIELDS));
        }
        String dir = filter.sortDir() == null || filter.sortDir().isBlank()
                ? "DESC"
                : filter.sortDir().trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(dir) && !"DESC".equals(dir)) {
            errors.add("sortDir must be ASC or DESC");
        }
        if (filter.eventType() != null
                && !filter.eventType().isBlank()) {
            try {
                AuditEventType.valueOf(filter.eventType().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                errors.add("eventType must be a valid AuditEventType name");
            }
        }
        return errors.isEmpty()
                ? new ValidatorDto(true, null, List.of())
                : new ValidatorDto(false, null, errors);
    }

    public ValidatorDto validateId(Long id) {
        if (id == null || id < 1) {
            return new ValidatorDto(false, null, List.of("id must be a positive number"));
        }
        return new ValidatorDto(true, null, List.of());
    }

    public ValidatorDto validateTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return new ValidatorDto(false, null, List.of("traceId is required"));
        }
        return new ValidatorDto(true, null, List.of());
    }

    public ValidatorDto validateServiceStats(String serviceName, int hours) {
        List<String> errors = new ArrayList<>();
        if (serviceName == null || serviceName.isBlank()) {
            errors.add("serviceName is required");
        }
        if (hours < 1 || hours > 8760) {
            errors.add("hours must be between 1 and 8760");
        }
        return errors.isEmpty()
                ? new ValidatorDto(true, null, List.of())
                : new ValidatorDto(false, null, errors);
    }
}
