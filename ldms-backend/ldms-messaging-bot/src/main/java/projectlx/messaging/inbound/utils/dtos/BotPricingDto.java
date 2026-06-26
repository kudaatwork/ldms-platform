package projectlx.messaging.inbound.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotPricingDto {
    private long assistantMessageCents;
    private long agentMessageCents;
    private long sessionStartCents;
    private long supportTicketOpenCents;
    private long liveChatMessageCents;
    private String currencyCode;
}
