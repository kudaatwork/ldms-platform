package projectlx.messaging.inbound.utils.responses;

import lombok.Getter;
import lombok.Setter;
import projectlx.messaging.inbound.utils.dtos.BotKnowledgeStatusDto;

@Getter
@Setter
public class BotKnowledgeResponse {
    private boolean success;
    private int statusCode;
    private String message;
    private BotKnowledgeStatusDto knowledgeStatus;
}
