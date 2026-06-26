package projectlx.messaging.inbound.business.logic.support;

import java.util.Map;

/**
 * Extension point for Lexi agent tools that read platform data or perform portal actions.
 */
public interface BotAgentTool {

    String name();

    String description();

    Map<String, Object> parameterSchema();

    String execute(BotAgentExecutionContext context, Map<String, Object> arguments);

    /** @return true when the tool creates or mutates platform data (Agent mode only). */
    default boolean mutating() {
        return false;
    }
}
