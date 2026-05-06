package projectlx.co.zw.notifications.utils.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.notifications.model.Channel;

import java.util.List;

@Getter
@Setter
@ToString
public class UpdateTemplateRequest {
    @NotNull(message = "Template ID cannot be null for an update")
    private Long id;

    private String templateKey;
    private String description;
    private List<Channel> channels;
    private String emailSubject;
    private String emailBodyHtml;
    private String smsBody;
    private String inAppTitle;
    private String inAppBody;
    private String whatsappTemplateName;
    private String whatsappBody;
    @JsonProperty("isActive")
    private Boolean active;
}
