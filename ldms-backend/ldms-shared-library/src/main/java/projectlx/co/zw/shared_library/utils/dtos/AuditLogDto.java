package projectlx.co.zw.shared_library.utils.dtos;

import projectlx.co.zw.shared_library.utils.enums.AuditEventType;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object for an audit log event.
 * This object will be sent over the message queue.
 */
public class AuditLogDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private String serviceName;
    /** @deprecated Prefer {@link #requestTimestamp} / {@link #responseTimestamp}; kept for queue compatibility. */
    @Deprecated
    private Instant timestamp;
    private Instant requestTimestamp;
    private Instant responseTimestamp;
    private String username;
    private String clientPlatform;
    private String clientIpAddress;
    private AuditEventType eventType;
    private String action;
    private String httpMethod;
    private String requestUrl;
    private Integer httpStatusCode;
    private Long responseTimeMs;
    private String requestPayload;
    private String responsePayload;
    private String curlCommand;
    private Map<String, String> requestHeaders;
    private String exceptionMessage;

    public static Builder builder() {
        return new Builder();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Deprecated
    public Instant getTimestamp() {
        return timestamp;
    }

    @Deprecated
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(Instant requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public Instant getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(Instant responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientPlatform() {
        return clientPlatform;
    }

    public void setClientPlatform(String clientPlatform) {
        this.clientPlatform = clientPlatform;
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getCurlCommand() {
        return curlCommand;
    }

    public void setCurlCommand(String curlCommand) {
        this.curlCommand = curlCommand;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    @Override
    public String toString() {
        return "AuditLogDto{traceId='" + traceId + "', serviceName='" + serviceName + "', action='" + action + "'}";
    }

    public static final class Builder {
        private String traceId;
        private String serviceName;
        private Instant timestamp;
        private Instant requestTimestamp;
        private Instant responseTimestamp;
        private String username;
        private String clientPlatform;
        private String clientIpAddress;
        private AuditEventType eventType;
        private String action;
        private String httpMethod;
        private String requestUrl;
        private Integer httpStatusCode;
        private Long responseTimeMs;
        private String requestPayload;
        private String responsePayload;
        private String curlCommand;
        private Map<String, String> requestHeaders;
        private String exceptionMessage;

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder requestTimestamp(Instant requestTimestamp) {
            this.requestTimestamp = requestTimestamp;
            return this;
        }

        public Builder responseTimestamp(Instant responseTimestamp) {
            this.responseTimestamp = responseTimestamp;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder clientPlatform(String clientPlatform) {
            this.clientPlatform = clientPlatform;
            return this;
        }

        public Builder clientIpAddress(String clientIpAddress) {
            this.clientIpAddress = clientIpAddress;
            return this;
        }

        public Builder eventType(AuditEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder requestUrl(String requestUrl) {
            this.requestUrl = requestUrl;
            return this;
        }

        public Builder httpStatusCode(Integer httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        public Builder responseTimeMs(Long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder requestPayload(String requestPayload) {
            this.requestPayload = requestPayload;
            return this;
        }

        public Builder responsePayload(String responsePayload) {
            this.responsePayload = responsePayload;
            return this;
        }

        public Builder curlCommand(String curlCommand) {
            this.curlCommand = curlCommand;
            return this;
        }

        public Builder requestHeaders(Map<String, String> requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }

        public Builder exceptionMessage(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
            return this;
        }

        public AuditLogDto build() {
            AuditLogDto dto = new AuditLogDto();
            dto.traceId = traceId;
            dto.serviceName = serviceName;
            dto.timestamp = timestamp;
            dto.requestTimestamp = requestTimestamp;
            dto.responseTimestamp = responseTimestamp;
            dto.username = username;
            dto.clientPlatform = clientPlatform;
            dto.clientIpAddress = clientIpAddress;
            dto.eventType = eventType;
            dto.action = action;
            dto.httpMethod = httpMethod;
            dto.requestUrl = requestUrl;
            dto.httpStatusCode = httpStatusCode;
            dto.responseTimeMs = responseTimeMs;
            dto.requestPayload = requestPayload;
            dto.responsePayload = responsePayload;
            dto.curlCommand = curlCommand;
            dto.requestHeaders = requestHeaders;
            dto.exceptionMessage = exceptionMessage;
            return dto;
        }
    }
}
