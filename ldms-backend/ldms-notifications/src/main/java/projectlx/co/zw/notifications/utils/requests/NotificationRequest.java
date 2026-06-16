package projectlx.co.zw.notifications.utils.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRequest implements Serializable {

    private String eventId;
    private String templateKey;
    private Recipient recipient;
    private Map<String, Object> data;
    private Metadata metadata;

    public NotificationRequest() {}

    public NotificationRequest(String eventId, String templateKey, Recipient recipient,
                                Map<String, Object> data, Metadata metadata) {
        this.eventId = eventId;
        this.templateKey = templateKey;
        this.recipient = recipient;
        this.data = data;
        this.metadata = metadata;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public Recipient getRecipient() { return recipient; }
    public void setRecipient(Recipient recipient) { this.recipient = recipient; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }

    @Override
    public String toString() {
        return "NotificationRequest{eventId='" + eventId + "', templateKey='" + templateKey + "'}";
    }

    public static class Recipient implements Serializable {
        private String userId;
        private String email;
        private String phoneNumber;
        private String fcmToken;
        private String slackWebhookUrl;
        private String teamsWebhookUrl;
        private Map<String, String> channelWebhookUrls;

        public Recipient() {}

        public Recipient(String userId, String email, String phoneNumber, String fcmToken,
                         String slackWebhookUrl, String teamsWebhookUrl,
                         Map<String, String> channelWebhookUrls) {
            this.userId = userId;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.fcmToken = fcmToken;
            this.slackWebhookUrl = slackWebhookUrl;
            this.teamsWebhookUrl = teamsWebhookUrl;
            this.channelWebhookUrls = channelWebhookUrls;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

        public String getSlackWebhookUrl() { return slackWebhookUrl; }
        public void setSlackWebhookUrl(String slackWebhookUrl) { this.slackWebhookUrl = slackWebhookUrl; }

        public String getTeamsWebhookUrl() { return teamsWebhookUrl; }
        public void setTeamsWebhookUrl(String teamsWebhookUrl) { this.teamsWebhookUrl = teamsWebhookUrl; }

        public Map<String, String> getChannelWebhookUrls() { return channelWebhookUrls; }
        public void setChannelWebhookUrls(Map<String, String> channelWebhookUrls) { this.channelWebhookUrls = channelWebhookUrls; }

        @Override
        public String toString() {
            return "Recipient{userId='" + userId + "', email='" + email + "'}";
        }
    }

    public static class Metadata implements Serializable {
        private String sourceService;
        private String traceId;

        public Metadata() {}

        public Metadata(String sourceService, String traceId) {
            this.sourceService = sourceService;
            this.traceId = traceId;
        }

        public String getSourceService() { return sourceService; }
        public void setSourceService(String sourceService) { this.sourceService = sourceService; }

        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }

        @Override
        public String toString() {
            return "Metadata{sourceService='" + sourceService + "', traceId='" + traceId + "'}";
        }
    }
}
