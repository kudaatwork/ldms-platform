package projectlx.co.zw.notifications.utils.requests;

import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

public class NotificationLogMultipleFiltersRequest extends MultipleFiltersRequest {

    private String templateKey;
    private String channel;
    private String status;
    /** Case-insensitive contains match on {@code NotificationLog.provider} (e.g. AWS_SES, TWILIO). */
    private String provider;
    private String recipientId;
    /** ISO-8601 date-time (inclusive). */
    private String from;
    /** ISO-8601 date-time (inclusive). */
    private String to;

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    @Override
    public String toString() {
        return "NotificationLogMultipleFiltersRequest{templateKey='" + templateKey
                + "', channel='" + channel + "', status='" + status + "'}";
    }
}
