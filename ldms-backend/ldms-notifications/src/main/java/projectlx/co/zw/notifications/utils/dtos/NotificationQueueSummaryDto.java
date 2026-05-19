package projectlx.co.zw.notifications.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NotificationQueueSummaryDto {

    private String queueName;
    private int messagesReady;
    private int messagesUnacked;
    private String exchangeName;
    private String routingKey;
}
