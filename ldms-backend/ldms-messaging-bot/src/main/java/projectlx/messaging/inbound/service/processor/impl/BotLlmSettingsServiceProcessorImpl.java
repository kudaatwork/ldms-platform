package projectlx.messaging.inbound.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.messaging.inbound.business.logic.api.BotLlmSettingsService;
import projectlx.messaging.inbound.service.processor.api.BotLlmSettingsServiceProcessor;
import projectlx.messaging.inbound.utils.requests.UpdateBotLlmRuntimeRequest;
import projectlx.messaging.inbound.utils.responses.BotLlmSettingsResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class BotLlmSettingsServiceProcessorImpl implements BotLlmSettingsServiceProcessor {

    private final BotLlmSettingsService botLlmSettingsService;

    @Override
    public BotLlmSettingsResponse currentSettings(Locale locale) {
        BotLlmSettingsResponse response = new BotLlmSettingsResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Bot LLM settings");
        response.setBotLlmSettingsDto(botLlmSettingsService.currentSettings(locale));
        return response;
    }

    @Override
    public BotLlmSettingsResponse updateRuntime(UpdateBotLlmRuntimeRequest request, Locale locale) {
        BotLlmSettingsResponse response = new BotLlmSettingsResponse();
        try {
            response.setBotLlmSettingsDto(botLlmSettingsService.updateRuntime(request, locale));
            response.setSuccess(true);
            response.setStatusCode(200);
            response.setMessage("Bot LLM runtime settings updated");
        } catch (IllegalArgumentException ex) {
            response.setSuccess(false);
            response.setStatusCode(400);
            response.setMessage(ex.getMessage());
        }
        return response;
    }
}
