package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.utils.enums.BotAssistantMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class BotAgentLlmBridge {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final BotLlmRouter botLlmRouter;
    private final GeminiLlmClient geminiClient;
    private final AnthropicLlmClient anthropicClient;
    private final String anthropicApiKey;
    private final String geminiApiKey;
    private final HttpClient httpClient;

    public BotAgentLlmBridge(ObjectMapper objectMapper,
                               BotLlmRouter botLlmRouter,
                               GeminiLlmClient geminiClient,
                               AnthropicLlmClient anthropicClient,
                               Environment environment) {
        this.objectMapper = objectMapper;
        this.botLlmRouter = botLlmRouter;
        this.geminiClient = geminiClient;
        this.anthropicClient = anthropicClient;
        this.anthropicApiKey = BotConfigPropertySupport.firstNonBlank(environment, "ldms.bot.llm.anthropic.api-key");
        this.geminiApiKey = BotConfigPropertySupport.firstNonBlank(environment,
                "ldms.bot.llm.gemini.api-key", "ldms.bot.gemini.api-key");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public boolean isConfigured() {
        return botLlmRouter.isConfigured();
    }

    public BotAgentTurnResult generateAgentTurn(String systemPrompt,
                                                ArrayNode conversation,
                                                BotAgentToolRegistry registry,
                                                BotAssistantMode mode) {
        if (!isConfigured()) {
            return new BotAgentTurnResult("", List.of());
        }
        String provider = botLlmRouter.resolvedProviderId();
        if ("anthropic".equals(provider)) {
            if (!anthropicClient.isConfigured() || anthropicApiKey.isBlank()) {
                return new BotAgentTurnResult("", List.of());
            }
            return generateAnthropicTurn(systemPrompt, conversation, registry, mode);
        }
        if ("gemini".equals(provider)) {
            if (!geminiClient.isConfigured() || geminiApiKey.isBlank()) {
                return new BotAgentTurnResult("", List.of());
            }
            return generateGeminiTurn(systemPrompt, conversation, registry, mode);
        }
        if (anthropicClient.isConfigured() && !anthropicApiKey.isBlank()) {
            return generateAnthropicTurn(systemPrompt, conversation, registry, mode);
        }
        if (geminiClient.isConfigured() && !geminiApiKey.isBlank()) {
            return generateGeminiTurn(systemPrompt, conversation, registry, mode);
        }
        return new BotAgentTurnResult("", List.of());
    }

    private BotAgentTurnResult generateAnthropicTurn(String systemPrompt,
                                                     ArrayNode conversation,
                                                     BotAgentToolRegistry registry,
                                                     BotAssistantMode mode) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", anthropicClient.modelId());
            body.put("max_tokens", 4096);
            body.put("system", systemPrompt == null ? "" : systemPrompt);
            body.set("messages", conversation);
            body.set("tools", objectMapper.valueToTree(registry.anthropicToolDefinitions(mode)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_API_URL))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", anthropicApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Anthropic agent API returned HTTP {}: {}", response.statusCode(), truncate(response.body(), 300));
                return new BotAgentTurnResult("", List.of());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("content");
            if (!content.isArray()) {
                return new BotAgentTurnResult("", List.of());
            }

            StringBuilder text = new StringBuilder();
            List<BotAgentToolCall> toolCalls = new ArrayList<>();
            for (JsonNode block : content) {
                String type = block.path("type").asText();
                if ("text".equals(type)) {
                    text.append(block.path("text").asText(""));
                } else if ("tool_use".equals(type)) {
                    Map<String, Object> input = objectMapper.convertValue(
                            block.path("input"),
                            objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
                    toolCalls.add(new BotAgentToolCall(
                            block.path("id").asText(UUID.randomUUID().toString()),
                            block.path("name").asText(""),
                            input == null ? Map.of() : input));
                }
            }
            return new BotAgentTurnResult(text.toString().trim(), toolCalls);
        } catch (Exception ex) {
            log.error("Anthropic agent turn failed", ex);
            return new BotAgentTurnResult("", List.of());
        }
    }

    private BotAgentTurnResult generateGeminiTurn(String systemPrompt,
                                                    ArrayNode conversation,
                                                    BotAgentToolRegistry registry,
                                                    BotAssistantMode mode) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode systemInstruction = body.putObject("systemInstruction");
            systemInstruction.putArray("parts").addObject().put("text", systemPrompt == null ? "" : systemPrompt);
            body.set("contents", conversation);
            ArrayNode tools = body.putArray("tools");
            ObjectNode functionDeclarations = tools.addObject();
            functionDeclarations.set("functionDeclarations",
                    objectMapper.valueToTree(registry.geminiFunctionDeclarations(mode)));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiClient.modelId() + ":generateContent?key=" + geminiApiKey();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini agent API returned HTTP {}: {}", response.statusCode(), truncate(response.body(), 300));
                return new BotAgentTurnResult("", List.of());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (!parts.isArray()) {
                return new BotAgentTurnResult("", List.of());
            }

            StringBuilder text = new StringBuilder();
            List<BotAgentToolCall> toolCalls = new ArrayList<>();
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    text.append(part.path("text").asText(""));
                }
                JsonNode functionCall = part.path("functionCall");
                if (!functionCall.isMissingNode()) {
                    Map<String, Object> input = objectMapper.convertValue(
                            functionCall.path("args"),
                            objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
                    toolCalls.add(new BotAgentToolCall(
                            UUID.randomUUID().toString(),
                            functionCall.path("name").asText(""),
                            input == null ? Map.of() : input));
                }
            }
            return new BotAgentTurnResult(text.toString().trim(), toolCalls);
        } catch (Exception ex) {
            log.error("Gemini agent turn failed", ex);
            return new BotAgentTurnResult("", List.of());
        }
    }

    public ArrayNode initialConversation(List<BotMessage> history) {
        ArrayNode conversation = objectMapper.createArrayNode();
        List<BotMessage> sanitized = BotLlmHistorySupport.sanitizeForLlm(history);
        for (BotMessage message : sanitized) {
            if (message.getRole() == BotMessageRole.SYSTEM) {
                continue;
            }
            ObjectNode turn = conversation.addObject();
            turn.put("role", message.getRole() == BotMessageRole.BOT ? "assistant" : "user");
            turn.put("content", message.getBody() == null ? "" : message.getBody());
        }
        return conversation;
    }

    public ArrayNode geminiInitialConversation(List<BotMessage> history) {
        ArrayNode conversation = objectMapper.createArrayNode();
        List<BotMessage> sanitized = BotLlmHistorySupport.sanitizeForLlm(history);
        for (BotMessage message : sanitized) {
            if (message.getRole() == BotMessageRole.SYSTEM) {
                continue;
            }
            ObjectNode turn = conversation.addObject();
            turn.put("role", message.getRole() == BotMessageRole.BOT ? "model" : "user");
            turn.putArray("parts").addObject().put("text", message.getBody() == null ? "" : message.getBody());
        }
        return conversation;
    }

    public void appendAnthropicAssistantTurn(ArrayNode conversation, BotAgentTurnResult turn) {
        ObjectNode assistant = conversation.addObject();
        assistant.put("role", "assistant");
        ArrayNode content = assistant.putArray("content");
        if (turn.text() != null && !turn.text().isBlank()) {
            content.addObject().put("type", "text").put("text", turn.text());
        }
        for (BotAgentToolCall call : turn.toolCalls()) {
            ObjectNode block = content.addObject();
            block.put("type", "tool_use");
            block.put("id", call.id());
            block.put("name", call.name());
            block.set("input", objectMapper.valueToTree(call.input()));
        }
    }

    public void appendAnthropicToolResults(ArrayNode conversation, List<BotAgentToolCall> calls, List<String> results) {
        ObjectNode user = conversation.addObject();
        user.put("role", "user");
        ArrayNode content = user.putArray("content");
        for (int i = 0; i < calls.size(); i++) {
            ObjectNode block = content.addObject();
            block.put("type", "tool_result");
            block.put("tool_use_id", calls.get(i).id());
            block.put("content", i < results.size() ? results.get(i) : "No result.");
        }
    }

    public void appendGeminiFunctionCall(ArrayNode conversation, BotAgentTurnResult turn) {
        ObjectNode model = conversation.addObject();
        model.put("role", "model");
        ArrayNode parts = model.putArray("parts");
        if (turn.text() != null && !turn.text().isBlank()) {
            parts.addObject().put("text", turn.text());
        }
        for (BotAgentToolCall call : turn.toolCalls()) {
            ObjectNode functionCall = parts.addObject().putObject("functionCall");
            functionCall.put("name", call.name());
            functionCall.set("args", objectMapper.valueToTree(call.input()));
        }
    }

    public void appendGeminiFunctionResponses(ArrayNode conversation,
                                              List<BotAgentToolCall> calls,
                                              List<String> results) {
        ObjectNode user = conversation.addObject();
        user.put("role", "user");
        ArrayNode parts = user.putArray("parts");
        for (int i = 0; i < calls.size(); i++) {
            ObjectNode functionResponse = parts.addObject().putObject("functionResponse");
            functionResponse.put("name", calls.get(i).name());
            ObjectNode response = functionResponse.putObject("response");
            response.put("result", i < results.size() ? results.get(i) : "No result.");
        }
    }

    public boolean usesAnthropic() {
        return "anthropic".equals(botLlmRouter.resolvedProviderId());
    }

    private String anthropicApiKey() {
        return anthropicApiKey;
    }

    private String geminiApiKey() {
        return geminiApiKey;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
