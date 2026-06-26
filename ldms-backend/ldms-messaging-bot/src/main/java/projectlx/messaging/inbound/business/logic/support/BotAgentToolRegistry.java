package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.utils.enums.BotAssistantMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BotAgentToolRegistry {

    private final List<BotAgentTool> tools;

    public BotAgentToolRegistry(List<BotAgentTool> tools) {
        this.tools = tools == null ? List.of() : List.copyOf(tools);
    }

    public List<BotAgentTool> tools() {
        return tools;
    }

    public BotAgentTool find(String name) {
        if (name == null) {
            return null;
        }
        return tools.stream()
                .filter(tool -> name.equals(tool.name()))
                .findFirst()
                .orElse(null);
    }

    public List<Map<String, Object>> anthropicToolDefinitions(BotAssistantMode mode) {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (BotAgentTool tool : toolsForMode(mode)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", tool.name());
            entry.put("description", tool.description());
            entry.put("input_schema", inputSchema(tool));
            definitions.add(entry);
        }
        return definitions;
    }

    public List<Map<String, Object>> geminiFunctionDeclarations(BotAssistantMode mode) {
        List<Map<String, Object>> declarations = new ArrayList<>();
        for (BotAgentTool tool : toolsForMode(mode)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", tool.name());
            entry.put("description", tool.description());
            entry.put("parameters", inputSchema(tool));
            declarations.add(entry);
        }
        return declarations;
    }

    /** @deprecated use {@link #anthropicToolDefinitions(BotAssistantMode)} */
    public List<Map<String, Object>> anthropicToolDefinitions() {
        return anthropicToolDefinitions(BotAssistantMode.AGENT);
    }

    /** @deprecated use {@link #geminiFunctionDeclarations(BotAssistantMode)} */
    public List<Map<String, Object>> geminiFunctionDeclarations() {
        return geminiFunctionDeclarations(BotAssistantMode.AGENT);
    }

    public List<BotAgentTool> toolsForMode(BotAssistantMode mode) {
        if (mode == BotAssistantMode.AGENT) {
            return tools;
        }
        return tools.stream().filter(tool -> !tool.mutating()).toList();
    }

    public BotAgentTool findForMode(String name, BotAssistantMode mode) {
        BotAgentTool tool = find(name);
        if (tool == null) {
            return null;
        }
        if (mode != BotAssistantMode.AGENT && tool.mutating()) {
            return null;
        }
        return tool;
    }

    private static Map<String, Object> inputSchema(BotAgentTool tool) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = tool.parameterSchema();
        schema.put("properties", properties == null ? Map.of() : properties);
        schema.put("required", requiredKeys(properties));
        return schema;
    }

    private static List<String> requiredKeys(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return List.of();
        }
        List<String> required = new ArrayList<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> property) {
                Object requiredFlag = property.get("required");
                if (Boolean.TRUE.equals(requiredFlag)) {
                    required.add(entry.getKey());
                }
            }
        }
        return required;
    }

    public static Map<String, Object> stringParam(String description, boolean required) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("type", "string");
        param.put("description", description);
        if (required) {
            param.put("required", true);
        }
        return param;
    }

    public static Map<String, Object> enumParam(String description, List<String> values, boolean required) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("type", "string");
        param.put("description", description);
        param.put("enum", values);
        if (required) {
            param.put("required", true);
        }
        return param;
    }

    public static Map<String, Object> longParam(String description, boolean required) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("type", "integer");
        param.put("description", description);
        if (required) {
            param.put("required", true);
        }
        return param;
    }

    public static long readLong(Map<String, Object> arguments, String key) {
        String raw = readString(arguments, key);
        if (raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public static String readString(Map<String, Object> arguments, String key) {
        if (arguments == null || key == null) {
            return "";
        }
        Object value = arguments.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static Locale locale(BotAgentExecutionContext context) {
        return context != null && context.locale() != null ? context.locale() : Locale.ENGLISH;
    }
}
