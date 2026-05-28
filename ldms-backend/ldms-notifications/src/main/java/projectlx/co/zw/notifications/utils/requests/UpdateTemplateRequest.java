package projectlx.co.zw.notifications.utils.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.notifications.model.Channel;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
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
