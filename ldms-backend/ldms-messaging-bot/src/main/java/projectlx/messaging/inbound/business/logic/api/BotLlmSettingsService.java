package projectlx.messaging.inbound.business.logic.api;

import projectlx.messaging.inbound.utils.dtos.BotLlmSettingsDto;
import projectlx.messaging.inbound.utils.requests.UpdateBotLlmRuntimeRequest;

import java.util.Locale;

public interface BotLlmSettingsService {

    BotLlmSettingsDto currentSettings(Locale locale);

    BotLlmSettingsDto updateRuntime(UpdateBotLlmRuntimeRequest request, Locale locale);
}
