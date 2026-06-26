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

public class ListUserGroupsTool implements BotAgentTool {

    private final UserManagementAgentClient userManagementAgentClient;
    private final ObjectMapper objectMapper;

    public ListUserGroupsTool(UserManagementAgentClient userManagementAgentClient, ObjectMapper objectMapper) {
        this.userManagementAgentClient = userManagementAgentClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "list_user_groups";
    }

    @Override
    public String description() {
        return "List user groups in the caller's organisation workspace (id, name, member counts).";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "searchTerm", BotAgentToolRegistry.stringParam(
                        "Optional name filter (partial match). Leave empty to list all visible groups.", false));
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("page", 0);
        request.put("size", 25);
        String searchTerm = BotAgentToolRegistry.readString(arguments, "searchTerm");
        if (!searchTerm.isBlank()) {
            request.put("name", searchTerm);
        }
        JsonNode response = userManagementAgentClient.listUserGroupsForUsername(
                context.username(), request, BotAgentToolRegistry.locale(context));
        return BotAgentJsonSupport.userGroupsText(response);
    }
}
