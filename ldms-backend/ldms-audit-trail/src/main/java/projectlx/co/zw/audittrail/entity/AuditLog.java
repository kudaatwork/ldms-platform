package projectlx.co.zw.audittrail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "audit_log")
@Immutable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String action;

    @Column(name = "client_ip_address", length = 255)
    private String clientIpAddress;

    @Column(name = "curl_command", columnDefinition = "LONGTEXT")
    private String curlCommand;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private AuditEventType eventType;

    @Column(name = "exception_message", columnDefinition = "LONGTEXT")
    private String exceptionMessage;

    @Column(name = "http_method", length = 255)
    private String httpMethod;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "request_url", length = 255)
    private String requestUrl;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "service_name", length = 255)
    private String serviceName;

    @Column(name = "request_timestamp")
    private LocalDateTime requestTimestamp;

    @Column(name = "response_timestamp")
    private LocalDateTime responseTimestamp;

    @Column(name = "trace_id", length = 255)
    private String traceId;

    @Column(length = 255)
    private String username;
}
