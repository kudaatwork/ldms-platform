package projectlx.co.zw.notifications.utils.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import projectlx.co.zw.notifications.model.Channel;

import java.util.List;
import java.util.Map;

public class UpdateTemplateRequest {
    @NotNull(message = "Template ID cannot be null for an update")
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
    private Boolean active;

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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    /** True when the client only toggles active/inactive (no template body fields). */
    public boolean isStatusOnlyUpdate() {
        if (active == null) {
            return false;
        }
        return hasNoTemplateBodyFields();
    }

    /** True when the client only updates per-channel delivery flags (e.g. table toggles). */
    public boolean isChannelDeliveryOnlyUpdate() {
        if (channelDeliveryEnabled == null || channelDeliveryEnabled.isEmpty()) {
            return false;
        }
        return active == null && hasNoTemplateBodyFieldsExceptDelivery();
    }

    private boolean hasNoTemplateBodyFields() {
        return hasNoTemplateBodyFieldsExceptDelivery()
                && (channelDeliveryEnabled == null || channelDeliveryEnabled.isEmpty());
    }

    private boolean hasNoTemplateBodyFieldsExceptDelivery() {
        return isBlank(templateKey)
                && isBlank(description)
                && (channels == null || channels.isEmpty())
                && isBlank(emailSubject)
                && isBlank(emailBodyHtml)
                && isBlank(smsBody)
                && isBlank(inAppTitle)
                && isBlank(inAppBody)
                && isBlank(whatsappTemplateName)
                && isBlank(whatsappBody);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
