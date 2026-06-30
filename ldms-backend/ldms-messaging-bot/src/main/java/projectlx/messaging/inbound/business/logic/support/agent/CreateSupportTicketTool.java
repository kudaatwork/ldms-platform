package projectlx.messaging.inbound.business.logic.support.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentJsonSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.clients.HelpSupportServiceClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateSupportTicketTool implements BotAgentTool {

    private final HelpSupportServiceClient helpSupportServiceClient;
    private final ObjectMapper objectMapper;

    public CreateSupportTicketTool(HelpSupportServiceClient helpSupportServiceClient, ObjectMapper objectMapper) {
        this.helpSupportServiceClient = helpSupportServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "create_support_ticket";
    }

    @Override
    public String description() {
        return "Open a Help & Support ticket on behalf of the user when human follow-up is required or an automated action cannot be completed. "
                + "Call only after the user explicitly confirmed (yes/go ahead) the draft subject and description.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("subject", BotAgentToolRegistry.stringParam("Short ticket subject (max 200 characters).", true));
        schema.put("description", BotAgentToolRegistry.stringParam(
                "Detailed description (min 20 characters) including PO/shipment/invoice references.", true));
        schema.put("category", BotAgentToolRegistry.enumParam(
                "Ticket category.",
                List.of("GENERAL", "TECHNICAL", "BILLING", "ACCESS", "SECURITY", "OPERATIONS"),
                true));
        return schema;
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        String subject = BotAgentToolRegistry.readString(arguments, "subject");
        String description = BotAgentToolRegistry.readString(arguments, "description");
        String category = BotAgentToolRegistry.readString(arguments, "category");
        if (subject.isBlank() || description.length() < 20) {
            return "subject and description (min 20 chars) are required to create a support ticket.";
        }
        if (category.isBlank()) {
            category = "GENERAL";
        }
        ObjectNode request = objectMapper.createObjectNode();
        request.put("subject", subject);
        request.put("description", description);
        request.put("category", category);
        JsonNode response = helpSupportServiceClient.createSupportTicketForUsername(
                context.username(), request, BotAgentToolRegistry.locale(context));
        return BotAgentJsonSupport.createdTicketText(response);
    }
}
