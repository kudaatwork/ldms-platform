package projectlx.co.zw.notifications.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.notifications.model.Channel;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationTemplateDto {
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
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
