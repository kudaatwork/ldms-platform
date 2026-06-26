package projectlx.messaging.inbound.business.logic.support.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentJsonSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.clients.UserManagementAgentClient;

import java.util.Map;

public class ListOrgUsersTool implements BotAgentTool {

    private final UserManagementAgentClient userManagementAgentClient;
    private final ObjectMapper objectMapper;

    public ListOrgUsersTool(UserManagementAgentClient userManagementAgentClient, ObjectMapper objectMapper) {
        this.userManagementAgentClient = userManagementAgentClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "list_org_users";
    }

    @Override
    public String description() {
        return "List users in the caller's organisation workspace (id, username, name, email).";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "searchTerm", BotAgentToolRegistry.stringParam(
                        "Optional filter on username, email, or name. Leave empty to list users.", false));
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("page", 0);
        request.put("size", 25);
        String searchTerm = BotAgentToolRegistry.readString(arguments, "searchTerm");
        if (!searchTerm.isBlank()) {
            request.put("searchValue", searchTerm);
        }
        JsonNode response = userManagementAgentClient.listUsersForUsername(
                context.username(), request, BotAgentToolRegistry.locale(context));
        return BotAgentJsonSupport.orgUsersText(response);
    }
}
