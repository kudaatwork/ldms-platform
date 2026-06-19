package projectlx.fleet.management.utils.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Envelope published to {@code notifications.direct / notifications.send} for
 * outbound notification dispatch.  Mirrors the same contract used by ldms-user-management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRequest implements Serializable {

    private String eventId;
    private String templateKey;
    private Recipient recipient;
    private Map<String, Object> data;
    private Metadata metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipient implements Serializable {
        private String userId;
        private String email;
        private String phoneNumber;
        private String fcmToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata implements Serializable {
        private String sourceService;
        private String traceId;
    }
}
