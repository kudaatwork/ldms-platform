package projectlx.messaging.inbound.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBotKnowledgeTextRequest {
    private String title;
    private String body;
}
