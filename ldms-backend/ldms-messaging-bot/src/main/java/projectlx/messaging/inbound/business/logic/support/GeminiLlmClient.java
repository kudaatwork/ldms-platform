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
public class GeminiLlmClient implements BotLlmClient {

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final BotLlmRuntimeSettings runtimeSettings;
    private final HttpClient httpClient;

    public GeminiLlmClient(ObjectMapper objectMapper,
                             String apiKey,
                             String model,
                             boolean enabled,
                             BotLlmRuntimeSettings runtimeSettings) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gemini-2.5-flash" : model.trim();
        this.enabled = enabled;
        this.runtimeSettings = runtimeSettings;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public String providerId() {
        return "gemini";
    }

    @Override
    public String modelId() {
        return effectiveModel();
    }

    private String effectiveModel() {
        if (runtimeSettings != null) {
            String override = runtimeSettings.geminiModelOverride();
            if (override != null && !override.isBlank()) {
                return override.trim();
            }
        }
        return model;
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
            ObjectNode systemInstruction = body.putObject("systemInstruction");
            ArrayNode systemParts = systemInstruction.putArray("parts");
            systemParts.addObject().put("text", systemPrompt);

            ArrayNode contents = body.putArray("contents");
            for (BotMessage message : conversation) {
                if (message.getRole() == BotMessageRole.SYSTEM) {
                    continue;
                }
                ObjectNode turn = contents.addObject();
                turn.put("role", message.getRole() == BotMessageRole.BOT ? "model" : "user");
                turn.putArray("parts").addObject().put("text", message.getBody());
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + effectiveModel() + ":generateContent?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API returned HTTP {}: {}", response.statusCode(), truncate(response.body(), 300));
                return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, isConfigured());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, isConfigured());
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, isConfigured());
            }
            String text = BotLlmHistorySupport.stripLegacyKeyNag(parts.get(0).path("text").asText("").trim());
            return text.isBlank()
                    ? BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, isConfigured())
                    : text;
        } catch (Exception ex) {
            log.error("Gemini generateContent failed", ex);
            return BotLlmFallbackSupport.replyFor(BotLlmFallbackSupport.lastUserMessage(conversation), conversation, isConfigured());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
