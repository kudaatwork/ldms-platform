package projectlx.messaging.inbound.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBotAssistantModeRequest {

    @NotBlank
    @Size(max = 32)
    private String sessionId;

    @NotBlank
    @Size(max = 50)
    private String assistantMode;
}
