package projectlx.messaging.inbound.business.logic.support.agent;

import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotCallerProfileSupport;

import java.util.LinkedHashMap;
import java.util.Map;

public class GetSessionContextTool implements BotAgentTool {

    private final BotCallerProfileSupport botCallerProfileSupport;

    public GetSessionContextTool(BotCallerProfileSupport botCallerProfileSupport) {
        this.botCallerProfileSupport = botCallerProfileSupport;
    }

    @Override
    public String name() {
        return "get_session_context";
    }

    @Override
    public String description() {
        return "Load the signed-in user's organisation, classification, and display profile for scoping LDMS actions.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of();
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        BotCallerProfileSupport.CallerProfile profile = botCallerProfileSupport.resolve(context.username());
        StringBuilder out = new StringBuilder();
        out.append("User: ").append(profile.displayName()).append(" (").append(context.username()).append(')');
        out.append("\nOrganisation: ").append(profile.organizationName());
        if (profile.organizationId() != null) {
            out.append(" (#").append(profile.organizationId()).append(')');
        }
        if (profile.organizationClassification() != null && !profile.organizationClassification().isBlank()) {
            out.append("\nClassification: ").append(profile.organizationClassification());
        }
        if (profile.phone() != null && !profile.phone().isBlank()) {
            out.append("\nPhone: ").append(profile.phone());
        }
        return out.toString();
    }
}
