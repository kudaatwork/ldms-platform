package projectlx.co.zw.notifications.utils.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.co.zw.notifications.model.Channel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationTemplateDto {
    private Long id;
    private String templateKey;
    private String description;
    private List<Channel> channels;
    private Map<String, Boolean> channelDeliveryEnabled;
    private String emailSubject;
    private String emailBodyHtml;
    private String smsBody;
    private String inAppTitle;
    private String inAppBody;
    private String whatsappTemplateName;
    private String whatsappBody;
    @JsonProperty("isActive")
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Channel> getChannels() { return channels; }
    public void setChannels(List<Channel> channels) { this.channels = channels; }

    public Map<String, Boolean> getChannelDeliveryEnabled() { return channelDeliveryEnabled; }
    public void setChannelDeliveryEnabled(Map<String, Boolean> channelDeliveryEnabled) { this.channelDeliveryEnabled = channelDeliveryEnabled; }

    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }

    public String getEmailBodyHtml() { return emailBodyHtml; }
    public void setEmailBodyHtml(String emailBodyHtml) { this.emailBodyHtml = emailBodyHtml; }

    public String getSmsBody() { return smsBody; }
    public void setSmsBody(String smsBody) { this.smsBody = smsBody; }

    public String getInAppTitle() { return inAppTitle; }
    public void setInAppTitle(String inAppTitle) { this.inAppTitle = inAppTitle; }

    public String getInAppBody() { return inAppBody; }
    public void setInAppBody(String inAppBody) { this.inAppBody = inAppBody; }

    public String getWhatsappTemplateName() { return whatsappTemplateName; }
    public void setWhatsappTemplateName(String whatsappTemplateName) { this.whatsappTemplateName = whatsappTemplateName; }

    public String getWhatsappBody() { return whatsappBody; }
    public void setWhatsappBody(String whatsappBody) { this.whatsappBody = whatsappBody; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "NotificationTemplateDto{id=" + id + ", templateKey='" + templateKey + "', channels=" + channels + "}";
    }
}
