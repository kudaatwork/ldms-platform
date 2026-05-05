package projectlx.co.zw.audittrail.utils.requests;

import java.time.LocalDateTime;
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
}
