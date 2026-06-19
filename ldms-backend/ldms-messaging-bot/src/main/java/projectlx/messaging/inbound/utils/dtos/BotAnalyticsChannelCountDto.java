package projectlx.messaging.inbound.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotAnalyticsChannelCountDto {
    private String channel;
    private long count;
}
