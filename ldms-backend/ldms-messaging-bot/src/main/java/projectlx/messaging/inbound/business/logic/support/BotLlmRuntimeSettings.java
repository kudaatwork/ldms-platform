package projectlx.messaging.inbound.business.logic.support;

import java.util.Locale;

/**
 * In-memory runtime overrides for bot LLM provider and model (admin-adjustable without restart).
 * API keys remain in environment variables only.
 */
public class BotLlmRuntimeSettings {

    private volatile String providerOverride;
    private volatile String geminiModelOverride;
    private volatile String anthropicModelOverride;

    public String providerOverride() {
        return providerOverride;
    }

    public void setProviderOverride(String providerOverride) {
        this.providerOverride = normalize(providerOverride);
    }

    public String geminiModelOverride() {
        return geminiModelOverride;
    }

    public void setGeminiModelOverride(String geminiModelOverride) {
        this.geminiModelOverride = normalize(geminiModelOverride);
    }

    public String anthropicModelOverride() {
        return anthropicModelOverride;
    }

    public void setAnthropicModelOverride(String anthropicModelOverride) {
        this.anthropicModelOverride = normalize(anthropicModelOverride);
    }

    public BotLlmRouter.ProviderMode effectiveProviderMode(BotLlmRouter.ProviderMode configured) {
        if (providerOverride == null || providerOverride.isBlank()) {
            return configured == null ? BotLlmRouter.ProviderMode.AUTO : configured;
        }
        return BotLlmRouter.ProviderMode.from(providerOverride);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String labelForProvider(String providerId) {
        if (providerId == null) {
            return "Unknown";
        }
        return switch (providerId.toLowerCase(Locale.ROOT)) {
            case "gemini", "google" -> "Gemini";
            case "anthropic", "claude" -> "Claude";
            case "auto" -> "Auto";
            default -> providerId;
        };
    }
}
