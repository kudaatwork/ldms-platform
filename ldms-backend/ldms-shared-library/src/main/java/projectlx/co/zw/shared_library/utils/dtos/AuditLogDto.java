package projectlx.co.zw.shared_library.utils.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.AuditEventType;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object for an audit log event.
 * This object will be sent over the message queue.
 * Using Lombok to reduce boilerplate code.
 */

@Getter
@Setter
@ToString
@Builder
public class AuditLogDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId; // For correlating logs across services
    private String serviceName; // The name of the microservice generating the log
    private Instant timestamp;
    private String username; // User who performed the action
    private String clientIpAddress;

    private AuditEventType eventType; // e.g., WEB_REQUEST, SERVICE_METHOD, FEIGN_CALL
    private String action; // From @Auditable or generated description

    // Web Request Details
    private String httpMethod;
    private String requestUrl;
    private Integer httpStatusCode;
    private Long responseTimeMs;
    private String requestPayload; // JSON string
    private String responsePayload; // JSON string
    private String curlCommand; // Generated cURL representation of the request
    private Map<String, String> requestHeaders;

    // For exceptions
    private String exceptionMessage;
}
