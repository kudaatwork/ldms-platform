package projectlx.messaging.inbound.business.logic.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some models emit {@code <tool_call>} XML in plain text instead of native tool-use blocks.
 * Parse those calls so Agent mode still executes real LDMS tools.
 */
public final class BotAgentTextToolCallSupport {

    private static final Pattern TOOL_CALL_BLOCK = Pattern.compile(
            "<tool_call>\\s*(\\{.*?\\})\\s*</tool_call>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TOOL_RESPONSE_BLOCK = Pattern.compile(
            "<tool_response>\\s*\\{.*?\\}\\s*</tool_response>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TOOL_CALLS_MADE_SECTION = Pattern.compile(
            "(?is)\\n*Tool calls made:\\s*.*?(?=\\n\\n---|\\n\\nI ran into|\\z)");
    private static final Pattern HALLUCINATED_TOOL_TRACE = Pattern.compile(
            "(?is)\\n*---\\s*\\n*I ran into a limitation.*?Assistant mode.*?(?=\\n\\nOption \\d|$|\\z)");

    private BotAgentTextToolCallSupport() {
    }

    public static List<BotAgentToolCall> parseFromText(String text, ObjectMapper objectMapper) {
        if (text == null || text.isBlank() || objectMapper == null) {
            return List.of();
        }
        List<BotAgentToolCall> calls = new ArrayList<>();
        Matcher matcher = TOOL_CALL_BLOCK.matcher(text);
        while (matcher.find()) {
            BotAgentToolCall call = parseJsonCall(matcher.group(1), objectMapper);
            if (call != null) {
                calls.add(call);
            }
        }
        return calls;
    }

    public static String stripToolMarkup(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String cleaned = text.trim();
        cleaned = TOOL_CALL_BLOCK.matcher(cleaned).replaceAll("");
        cleaned = TOOL_RESPONSE_BLOCK.matcher(cleaned).replaceAll("");
        cleaned = TOOL_CALLS_MADE_SECTION.matcher(cleaned).replaceAll("");
        cleaned = HALLUCINATED_TOOL_TRACE.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replaceAll("(?m)^\\s*---\\s*$", "");
        return cleaned.replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static BotAgentToolCall parseJsonCall(String json, ObjectMapper objectMapper) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String name = node.path("name").asText("").trim();
            if (name.isBlank()) {
                return null;
            }
            JsonNode argsNode = node.has("arguments") ? node.get("arguments")
                    : node.has("input") ? node.get("input") : node.get("args");
            Map<String, Object> input = new LinkedHashMap<>();
            if (argsNode != null && argsNode.isObject()) {
                argsNode.fields().forEachRemaining(entry ->
                        input.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
            }
            return new BotAgentToolCall(UUID.randomUUID().toString(), name, input);
        } catch (Exception ignored) {
            return null;
        }
    }
}
