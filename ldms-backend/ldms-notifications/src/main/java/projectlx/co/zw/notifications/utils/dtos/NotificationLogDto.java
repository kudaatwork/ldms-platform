package projectlx.co.zw.notifications.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.notifications.model.Channel;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class NotificationLogDto {

    private Long id;
    private String eventId;
    private String recipientId;
    private String recipientDisplay;
    private String recipientEmail;
    private String recipientPhone;
    private String templateKey;
    private Channel channel;
    private String status;
    private String provider;
    private String providerMessageId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
