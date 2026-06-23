package projectlx.messaging.inbound.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotAnalyticsMessageDailyCountDto {
    private String date;
    private long userMessages;
    private long botMessages;
}
