package projectlx.co.zw.audittrail.utils.requests;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Query parameters for searching audit logs (system / frontend / backend APIs).
 */
@Data
public class AuditLogSearchRequest {

    private String serviceName;
    private String username;
    private String eventType;
    private Integer httpStatusCode;
    private LocalDateTime from;
    private LocalDateTime to;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
}
