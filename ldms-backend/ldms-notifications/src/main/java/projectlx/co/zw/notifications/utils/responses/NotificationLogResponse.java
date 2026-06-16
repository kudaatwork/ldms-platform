package projectlx.co.zw.notifications.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;
import projectlx.co.zw.notifications.utils.dtos.NotificationLogDto;
import projectlx.co.zw.notifications.utils.dtos.NotificationQueueSummaryDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationLogResponse extends CommonResponse {

    private Page<NotificationLogDto> notificationLogPage;
    private NotificationQueueSummaryDto queueSummary;

    public Page<NotificationLogDto> getNotificationLogPage() { return notificationLogPage; }
    public void setNotificationLogPage(Page<NotificationLogDto> notificationLogPage) { this.notificationLogPage = notificationLogPage; }

    public NotificationQueueSummaryDto getQueueSummary() { return queueSummary; }
    public void setQueueSummary(NotificationQueueSummaryDto queueSummary) { this.queueSummary = queueSummary; }

    @Override
    public String toString() {
        return "NotificationLogResponse{statusCode=" + getStatusCode()
                + ", notificationLogPage=" + notificationLogPage
                + ", queueSummary=" + queueSummary + "}";
    }
}
