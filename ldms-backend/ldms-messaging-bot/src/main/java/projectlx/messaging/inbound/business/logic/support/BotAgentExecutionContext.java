package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.utils.enums.BotAssistantMode;

import java.util.Locale;

/** Caller context passed to {@link BotAgentTool} executions. */
public record BotAgentExecutionContext(
        String username,
        Long organizationId,
        String organizationClassification,
        Locale locale,
        BotAssistantMode assistantMode
) {
    public boolean agentMode() {
        return assistantMode == BotAssistantMode.AGENT;
    }
}
