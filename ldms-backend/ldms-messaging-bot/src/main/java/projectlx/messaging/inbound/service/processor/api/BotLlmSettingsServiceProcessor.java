package projectlx.messaging.inbound.service.processor.api;

import projectlx.messaging.inbound.utils.requests.UpdateBotLlmRuntimeRequest;
import projectlx.messaging.inbound.utils.responses.BotLlmSettingsResponse;

import java.util.Locale;

public interface BotLlmSettingsServiceProcessor {

    BotLlmSettingsResponse currentSettings(Locale locale);

    BotLlmSettingsResponse updateRuntime(UpdateBotLlmRuntimeRequest request, Locale locale);
}
