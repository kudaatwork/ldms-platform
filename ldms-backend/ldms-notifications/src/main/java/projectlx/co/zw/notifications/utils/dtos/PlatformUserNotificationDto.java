package projectlx.co.zw.notifications.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformUserNotificationDto {

    private Long id;
    private Long userId;
    private Long organizationId;
    private String eventKey;
    private String title;
    private String body;
    private String actionRoute;
    private String entityType;
    private Long entityId;
    private String sourceEventId;
    private LocalDateTime readAt;
    private LocalDateTime dismissedAt;
    private LocalDateTime createdAt;
    private boolean unread;
}
