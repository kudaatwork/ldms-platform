package projectlx.messaging.inbound.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditBotFaqRequest {
    private String question;
    private String answer;
    private String category;
    private String keywords;
    private Boolean published;
}
