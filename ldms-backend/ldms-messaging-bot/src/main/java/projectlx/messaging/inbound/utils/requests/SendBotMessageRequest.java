package projectlx.messaging.inbound.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendBotMessageRequest {

    @NotBlank
    @Size(max = 32)
    private String sessionId;

    @NotBlank
    @Size(max = 4000)
    private String body;

    /** When set, persisted on the session before the message is processed (matches UI toggle). */
    @Size(max = 50)
    private String assistantMode;
}
