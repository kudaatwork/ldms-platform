package projectlx.messaging.inbound.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBotLlmRuntimeRequest {
    /** auto | gemini | anthropic — blank clears runtime override */
    private String provider;
    private String geminiModel;
    private String anthropicModel;
}
