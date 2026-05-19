package projectlx.co.zw.notifications.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.notifications.utils.dtos.NotificationLogDto;
import projectlx.co.zw.notifications.utils.dtos.NotificationQueueSummaryDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationLogResponse extends CommonResponse {

    private Page<NotificationLogDto> notificationLogPage;
    private NotificationQueueSummaryDto queueSummary;
}
