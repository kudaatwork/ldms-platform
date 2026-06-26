package projectlx.messaging.inbound.utils.enums;

import java.util.Locale;

public enum BotAssistantMode {
    ASSISTANT("Assistant"),
    AGENT("Agent");

    private final String label;

    BotAssistantMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BotAssistantMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ASSISTANT;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "AGENT" -> AGENT;
            default -> ASSISTANT;
        };
    }
}
