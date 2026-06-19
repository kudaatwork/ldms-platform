package projectlx.messaging.inbound.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotAnalyticsTopicCountDto {
    private String topic;
    private long count;
}
