package projectlx.co.zw.notificationsmanagementservice.utils.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Map;

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
        private String slackWebhookUrl;
        private String teamsWebhookUrl;
        private Map<String, String> channelWebhookUrls;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata implements Serializable {
        private String sourceService;
        private String traceId;
    }
}