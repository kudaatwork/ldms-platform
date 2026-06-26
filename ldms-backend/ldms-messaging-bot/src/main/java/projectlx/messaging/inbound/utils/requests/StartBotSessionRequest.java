package projectlx.messaging.inbound.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartBotSessionRequest {

    @Size(max = 200)
    private String topic;

    @Size(max = 50)
    private String assistantMode;
}
