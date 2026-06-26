package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import projectlx.messaging.inbound.model.BotMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Routes bot replies to Gemini, Anthropic Claude, or auto-selects the first configured provider.
 */
@Slf4j
public class BotLlmRouter implements BotLlmClient {

    public enum ProviderMode {
        GEMINI,
        ANTHROPIC,
        AUTO;

        public static ProviderMode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return AUTO;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "gemini", "google" -> GEMINI;
                case "anthropic", "claude" -> ANTHROPIC;
                default -> AUTO;
            };
        }

        public String wireValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final ProviderMode configuredMode;
    private final BotLlmRuntimeSettings runtimeSettings;
    private final GeminiLlmClient geminiClient;
    private final AnthropicLlmClient anthropicClient;
    private final BotGuideModeSupport botGuideModeSupport;
    private volatile BotLlmClient activeClient;

    public BotLlmRouter(ProviderMode configuredMode,
                        BotLlmRuntimeSettings runtimeSettings,
                        GeminiLlmClient geminiClient,
                        AnthropicLlmClient anthropicClient,
                        BotGuideModeSupport botGuideModeSupport) {
        this.configuredMode = configuredMode == null ? ProviderMode.AUTO : configuredMode;
        this.runtimeSettings = runtimeSettings;
        this.geminiClient = geminiClient;
        this.anthropicClient = anthropicClient;
        this.botGuideModeSupport = botGuideModeSupport;
        this.activeClient = resolveActiveClient();
    }

    public ProviderMode configuredMode() {
        return configuredMode;
    }

    @Override
    public String providerId() {
        return resolveActiveClient().providerId();
    }

    @Override
    public String modelId() {
        return resolveActiveClient().modelId();
    }

    @Override
    public boolean isConfigured() {
        return geminiClient.isConfigured() || anthropicClient.isConfigured();
    }

    @Override
    public String generateReply(String systemPrompt, List<BotMessage> history) {
        List<BotMessage> conversation = BotLlmHistorySupport.sanitizeForLlm(history);
        BotLlmClient client = resolveActiveClient();
        activeClient = client;
        if (!client.isConfigured()) {
            return guideModeReply(conversation);
        }
        if (effectiveMode() == ProviderMode.AUTO) {
            return BotResponseSanitizer.forUserDisplay(generateWithAutoFailover(systemPrompt, conversation));
        }
        String reply = BotResponseSanitizer.forUserDisplay(
                BotLlmHistorySupport.stripLegacyKeyNag(client.generateReply(systemPrompt, conversation)));
        if (isObviousFallbackOnly(reply, conversation)) {
            log.warn("LLM provider {} returned fallback-shaped reply; using guide mode", client.providerId());
            return guideModeReply(conversation);
        }
        return reply;
    }

    private String generateWithAutoFailover(String systemPrompt, List<BotMessage> conversation) {
        List<BotLlmClient> candidates = orderedAutoClients();
        for (BotLlmClient client : candidates) {
            if (!client.isConfigured()) {
                continue;
            }
            String reply = BotLlmHistorySupport.stripLegacyKeyNag(client.generateReply(systemPrompt, conversation));
            if (!isObviousFallbackOnly(reply, conversation)) {
                activeClient = client;
                return reply;
            }
            log.warn("LLM provider {} returned fallback-shaped reply; trying next provider", client.providerId());
        }
        return guideModeReply(conversation);
    }

    private String guideModeReply(List<BotMessage> conversation) {
        String latest = BotLlmFallbackSupport.lastUserMessage(conversation);
        if (botGuideModeSupport != null) {
            return botGuideModeSupport.assistantReply(latest, conversation, false);
        }
        return BotLlmFallbackSupport.replyFor(latest, conversation, false);
    }

    private List<BotLlmClient> orderedAutoClients() {
        List<BotLlmClient> clients = new ArrayList<>();
        if (anthropicClient.isConfigured()) {
            clients.add(anthropicClient);
        }
        if (geminiClient.isConfigured()) {
            clients.add(geminiClient);
        }
        return clients;
    }

    private ProviderMode effectiveMode() {
        if (runtimeSettings == null) {
            return configuredMode;
        }
        return runtimeSettings.effectiveProviderMode(configuredMode);
    }

    private BotLlmClient resolveActiveClient() {
        BotLlmRouter.ProviderMode mode = effectiveMode();
        return switch (mode) {
            case GEMINI -> geminiClient;
            case ANTHROPIC -> anthropicClient;
            case AUTO -> {
                if (anthropicClient.isConfigured()) {
                    yield anthropicClient;
                }
                if (geminiClient.isConfigured()) {
                    yield geminiClient;
                }
                yield geminiClient;
            }
        };
    }

    /** Provider id that will be used for the next reply (respects runtime overrides). */
    public String resolvedProviderId() {
        return resolveActiveClient().providerId();
    }

    /** Model id that will be used for the next reply (respects runtime overrides). */
    public String resolvedModelId() {
        return resolveActiveClient().modelId();
    }

    public void syncActiveClient() {
        this.activeClient = resolveActiveClient();
    }

    private boolean isObviousFallbackOnly(String reply, List<BotMessage> conversation) {
        if (reply == null || reply.isBlank()) {
            return true;
        }
        String expected = BotLlmFallbackSupport.replyFor(
                BotLlmFallbackSupport.lastUserMessage(conversation), conversation, isConfigured());
        return reply.trim().equals(expected.trim());
    }
}
