package projectlx.co.zw.notifications.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.notifications.utils.dtos.PlatformUserNotificationDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformUserNotificationResponse extends CommonResponse {

    private PlatformUserNotificationDto notificationDto;
    private List<PlatformUserNotificationDto> notificationDtoList;
}
