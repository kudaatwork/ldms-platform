package projectlx.messaging.inbound.utils.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BotLlmSettingsDto {
    private String configuredProvider;
    private String activeProvider;
    private String activeModel;
    private boolean geminiConfigured;
    private boolean anthropicConfigured;
    private String runtimeProvider;
    private String runtimeGeminiModel;
    private String runtimeAnthropicModel;
    private List<BotLlmModelOptionDto> modelCatalog = new ArrayList<>();
}
