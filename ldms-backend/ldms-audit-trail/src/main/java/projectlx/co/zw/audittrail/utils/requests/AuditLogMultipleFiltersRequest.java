package projectlx.co.zw.audittrail.utils.requests;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class AuditLogMultipleFiltersRequest extends MultipleFiltersRequest {

    private String serviceName;
    private String username;
    private String eventType;
    private Integer httpStatusCode;
    private LocalDateTime from;
    private LocalDateTime to;
    private String sortBy;
    private String sortDir;

    /** Substring match (case-insensitive); optional. */
    private String action;

    private String requestUrl;
    private String httpMethod;
    private String traceId;

    /** Exact match on originating client (e.g. ADMIN_PORTAL). */
    private String clientPlatform;

    /** When non-empty, {@code action} must be one of these values. */
    private List<String> actionsIn;

    /** When non-empty, rows whose {@code action} is in this list are excluded. */
    private List<String> excludeActions;

    /** When non-empty, only rows whose {@code username} is in this list are returned. */
    private List<String> usernamesIn;
}
