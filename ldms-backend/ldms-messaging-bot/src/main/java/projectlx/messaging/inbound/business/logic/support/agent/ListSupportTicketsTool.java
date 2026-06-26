package projectlx.messaging.inbound.business.logic.support.agent;

import com.fasterxml.jackson.databind.JsonNode;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentJsonSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.clients.HelpSupportServiceClient;

import java.util.Map;

public class ListSupportTicketsTool implements BotAgentTool {

    private final HelpSupportServiceClient helpSupportServiceClient;

    public ListSupportTicketsTool(HelpSupportServiceClient helpSupportServiceClient) {
        this.helpSupportServiceClient = helpSupportServiceClient;
    }

    @Override
    public String name() {
        return "list_support_tickets";
    }

    @Override
    public String description() {
        return "List the user's Help & Support tickets (subject, status, ticket number).";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of();
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        JsonNode response = helpSupportServiceClient.listSupportTicketsByUsername(
                context.username(), BotAgentToolRegistry.locale(context));
        return BotAgentJsonSupport.supportTicketsText(response);
    }
}
