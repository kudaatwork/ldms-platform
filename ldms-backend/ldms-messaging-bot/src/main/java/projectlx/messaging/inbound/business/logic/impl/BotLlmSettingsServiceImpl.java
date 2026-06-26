package projectlx.messaging.inbound.business.logic.impl;

import lombok.RequiredArgsConstructor;
import projectlx.messaging.inbound.business.logic.api.BotLlmSettingsService;
import projectlx.messaging.inbound.business.logic.support.BotLlmSettingsSupport;
import projectlx.messaging.inbound.utils.dtos.BotLlmSettingsDto;
import projectlx.messaging.inbound.utils.requests.UpdateBotLlmRuntimeRequest;

import java.util.Locale;

@RequiredArgsConstructor
public class BotLlmSettingsServiceImpl implements BotLlmSettingsService {

    private final BotLlmSettingsSupport botLlmSettingsSupport;

    @Override
    public BotLlmSettingsDto currentSettings(Locale locale) {
        return botLlmSettingsSupport.currentSettings();
    }

    @Override
    public BotLlmSettingsDto updateRuntime(UpdateBotLlmRuntimeRequest request, Locale locale) {
        return botLlmSettingsSupport.updateRuntime(request);
    }
}
