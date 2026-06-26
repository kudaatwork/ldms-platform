package projectlx.messaging.inbound.business.logic.support;

import java.util.Map;

public record BotAgentToolCall(String id, String name, Map<String, Object> input) {
}
