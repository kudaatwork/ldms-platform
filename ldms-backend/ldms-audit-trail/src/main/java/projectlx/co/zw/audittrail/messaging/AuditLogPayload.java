package projectlx.co.zw.audittrail.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import projectlx.co.zw.audittrail.entity.AuditEventType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogPayload {

    private String traceId;
    private String serviceName;
    private String username;
    private String clientIpAddress;
    private String action;
    private AuditEventType eventType;
    private String httpMethod;
    private String requestUrl;
    private String requestHeaders;
    private String requestPayload;
    private String responsePayload;
    private Integer httpStatusCode;
    private Long responseTimeMs;
    private String curlCommand;
    private String exceptionMessage;
    private LocalDateTime requestTimestamp;
    private LocalDateTime responseTimestamp;
}
