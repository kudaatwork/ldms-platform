package projectlx.messaging.inbound.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotSessionDto {
    private String sessionId;
    private String userDisplayName;
    private String userPhone;
    private String organizationName;
    private String channel;
    private String status;
    private String statusLabel;
    private String topic;
    private String assistantMode;
    private String assistantModeLabel;
    private String lastMessageAt;
    private Integer messageCount;
    private Integer satisfactionScore;
    private List<BotMessageDto> messages;
}
