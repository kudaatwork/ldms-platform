package projectlx.messaging.inbound.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotLlmModelOptionDto {
    private String providerId;
    private String modelId;
    private String label;
}
