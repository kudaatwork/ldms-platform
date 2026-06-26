package projectlx.messaging.inbound.business.logic.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
public class AnthropicLlmClient implements BotLlmClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final BotLlmRuntimeSettings runtimeSettings;
    private final HttpClient httpClient;

    public AnthropicLlmClient(ObjectMapper objectMapper,
                              String apiKey,
                              String model,
                              boolean enabled,
                              BotLlmRuntimeSettings runtimeSettings) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "claude-sonnet-4-6" : model.trim();
        this.enabled = enabled;
        this.runtimeSettings = runtimeSettings;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public String providerId() {
        return "anthropic";
    }

    @Override
    public String modelId() {
        return effectiveModel();
    }

    private String effectiveModel() {
        if (runtimeSettings != null) {
            String override = runtimeSettings.anthropicModelOverride();
            if (override != null && !override.isBlank()) {
                return migrateRetiredModel(override.trim());
            }
        }
        return model;
    }

    private static String migrateRetiredModel(String modelId) {
        return switch (modelId) {
            case "claude-sonnet-4-20250514", "claude-sonnet-4-0", "claude-3-5-sonnet-20241022" -> "claude-sonnet-4-6";
            case "claude-opus-4-20250514", "claude-opus-4-0" -> "claude-opus-4-8";
            case "claude-3-5-haiku-20241022" -> "claude-haiku-4-5";
            default -> modelId;
        };
    }

    @Override
    public boolean isConfigured() {
        return enabled && !apiKey.isBlank();
    }

    @Override
    public String generateReply(String systemPrompt, List<BotMessage> history) {
        List<BotMessage> conversation = BotLlmHistorySupport.sanitizeForLlm(history);
        if (!isConfigured()) {
            return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, isConfigured());
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", effectiveModel());
            body.put("max_tokens", 4096);
            body.put("system", systemPrompt == null ? "" : systemPrompt);

            ArrayNode messages = body.putArray("messages");
            for (BotMessage message : conversation) {
                if (message.getRole() == BotMessageRole.SYSTEM) {
                    continue;
                }
                ObjectNode turn = messages.addObject();
                turn.put("role", message.getRole() == BotMessageRole.BOT ? "assistant" : "user");
                turn.put("content", message.getBody() == null ? "" : message.getBody());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Anthropic API returned HTTP {}: {}", response.statusCode(), truncate(response.body(), 300));
                return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, false);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, false);
            }
            StringBuilder text = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText(""));
                }
            }
            String reply = BotLlmHistorySupport.stripLegacyKeyNag(text.toString().trim());
            return reply.isBlank()
                    ? BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, false)
                    : reply;
        } catch (Exception ex) {
            log.error("Anthropic messages API failed", ex);
            return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, false);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
