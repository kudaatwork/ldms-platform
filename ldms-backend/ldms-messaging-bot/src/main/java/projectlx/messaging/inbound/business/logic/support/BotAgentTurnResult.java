package projectlx.messaging.inbound.business.logic.support;

import java.util.List;

public record BotAgentTurnResult(String text, List<BotAgentToolCall> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
