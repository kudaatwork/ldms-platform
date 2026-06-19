package projectlx.messaging.inbound.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotAnalyticsDailyCountDto {
    private String date;
    private long count;
}
