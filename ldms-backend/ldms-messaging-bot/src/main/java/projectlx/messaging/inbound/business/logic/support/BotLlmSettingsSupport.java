package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.utils.dtos.BotLlmModelOptionDto;
import projectlx.messaging.inbound.utils.dtos.BotLlmSettingsDto;
import projectlx.messaging.inbound.utils.requests.UpdateBotLlmRuntimeRequest;

import java.util.ArrayList;
import java.util.List;

public class BotLlmSettingsSupport {

    private final BotLlmRouter configuredRouter;
    private final BotLlmRuntimeSettings runtimeSettings;
    private final GeminiLlmClient geminiClient;
    private final AnthropicLlmClient anthropicClient;
    private final BotLlmRouter.ProviderMode configuredProviderMode;

    public BotLlmSettingsSupport(BotLlmRouter configuredRouter,
                                 BotLlmRuntimeSettings runtimeSettings,
                                 GeminiLlmClient geminiClient,
                                 AnthropicLlmClient anthropicClient,
                                 BotLlmRouter.ProviderMode configuredProviderMode) {
        this.configuredRouter = configuredRouter;
        this.runtimeSettings = runtimeSettings;
        this.geminiClient = geminiClient;
        this.anthropicClient = anthropicClient;
        this.configuredProviderMode = configuredProviderMode == null
                ? BotLlmRouter.ProviderMode.AUTO
                : configuredProviderMode;
    }

    public BotLlmSettingsDto currentSettings() {
        BotLlmSettingsDto dto = new BotLlmSettingsDto();
        dto.setConfiguredProvider(configuredProviderMode.name().toLowerCase());
        dto.setGeminiConfigured(geminiClient.isConfigured());
        dto.setAnthropicConfigured(anthropicClient.isConfigured());
        dto.setRuntimeProvider(runtimeSettings.providerOverride());
        dto.setRuntimeGeminiModel(runtimeSettings.geminiModelOverride());
        dto.setRuntimeAnthropicModel(runtimeSettings.anthropicModelOverride());
        dto.setActiveProvider(configuredRouter.resolvedProviderId());
        dto.setActiveModel(configuredRouter.resolvedModelId());
        dto.setModelCatalog(modelCatalog());
        return dto;
    }

    public BotLlmSettingsDto updateRuntime(UpdateBotLlmRuntimeRequest request) {
        if (request != null) {
            migrateRetiredModels(request);
            validateRuntimeUpdate(request);
            runtimeSettings.setProviderOverride(request.getProvider());
            runtimeSettings.setGeminiModelOverride(request.getGeminiModel());
            runtimeSettings.setAnthropicModelOverride(request.getAnthropicModel());
            configuredRouter.syncActiveClient();
        }
        return currentSettings();
    }

    private void migrateRetiredModels(UpdateBotLlmRuntimeRequest request) {
        String anthropic = request.getAnthropicModel();
        if (anthropic == null || anthropic.isBlank()) {
            return;
        }
        String normalized = anthropic.trim();
        if ("claude-sonnet-4-20250514".equals(normalized) || "claude-sonnet-4-0".equals(normalized)) {
            request.setAnthropicModel("claude-sonnet-4-6");
        } else if ("claude-opus-4-20250514".equals(normalized) || "claude-opus-4-0".equals(normalized)) {
            request.setAnthropicModel("claude-opus-4-8");
        } else if ("claude-3-5-sonnet-20241022".equals(normalized) || "claude-3-5-haiku-20241022".equals(normalized)) {
            request.setAnthropicModel("claude-sonnet-4-6");
        }
    }

    private void validateRuntimeUpdate(UpdateBotLlmRuntimeRequest request) {
        String provider = normalizeProvider(request.getProvider());
        if ("gemini".equals(provider) && !geminiClient.isConfigured()) {
            throw new IllegalArgumentException(
                    "Gemini is not configured on this server. Set GEMINI_API_KEY or ldms.bot.llm.gemini.api-key.");
        }
        if ("anthropic".equals(provider) && !anthropicClient.isConfigured()) {
            throw new IllegalArgumentException(
                    "Anthropic is not configured on this server. Set ANTHROPIC_API_KEY or ldms.bot.llm.anthropic.api-key.");
        }
        if ("gemini".equals(provider) && isBlank(request.getGeminiModel())) {
            throw new IllegalArgumentException("Select a Gemini model before applying.");
        }
        if ("anthropic".equals(provider) && isBlank(request.getAnthropicModel())) {
            throw new IllegalArgumentException("Select a Claude model before applying.");
        }
    }

    private static String normalizeProvider(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<BotLlmModelOptionDto> modelCatalog() {
        List<BotLlmModelOptionDto> catalog = new ArrayList<>();
        catalog.add(option("gemini", "gemini-2.5-flash", "Gemini 2.5 Flash"));
        catalog.add(option("gemini", "gemini-2.5-pro", "Gemini 2.5 Pro"));
        catalog.add(option("gemini", "gemini-2.0-flash", "Gemini 2.0 Flash"));
        catalog.add(option("anthropic", "claude-sonnet-4-6", "Claude Sonnet 4.6"));
        catalog.add(option("anthropic", "claude-opus-4-8", "Claude Opus 4.8"));
        catalog.add(option("anthropic", "claude-haiku-4-5", "Claude Haiku 4.5"));
        catalog.add(option("anthropic", "claude-sonnet-4-5", "Claude Sonnet 4.5"));
        return catalog;
    }

    private static BotLlmModelOptionDto option(String providerId, String modelId, String label) {
        BotLlmModelOptionDto dto = new BotLlmModelOptionDto();
        dto.setProviderId(providerId);
        dto.setModelId(modelId);
        dto.setLabel(label);
        return dto;
    }
}
