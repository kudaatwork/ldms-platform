package projectlx.messaging.inbound.business.logic.support.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentJsonSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.clients.UserManagementAgentClient;

import java.util.Map;

@Slf4j
public class CreateUserGroupTool implements BotAgentTool {

    private final UserManagementAgentClient userManagementAgentClient;
    private final ObjectMapper objectMapper;

    public CreateUserGroupTool(UserManagementAgentClient userManagementAgentClient, ObjectMapper objectMapper) {
        this.userManagementAgentClient = userManagementAgentClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "create_user_group";
    }

    @Override
    public String description() {
        return "Create a new user group in the caller's organisation workspace. Requires a group name.";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "name", BotAgentToolRegistry.stringParam(
                        "User group name (will be stored uppercase). Example: Operations Management.", true),
                "description", BotAgentToolRegistry.stringParam(
                        "Optional description of the group's purpose.", false));
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        String name = BotAgentToolRegistry.readString(arguments, "name");
        if (name.isBlank()) {
            return "A group name is required to create a user group.";
        }
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", name);
        String description = BotAgentToolRegistry.readString(arguments, "description");
        if (!description.isBlank()) {
            request.put("description", description);
        }
        if (context.organizationId() != null) {
            request.put("organizationId", context.organizationId());
        }
        if (context.organizationClassification() != null && !context.organizationClassification().isBlank()) {
            request.put("organizationClassification", context.organizationClassification());
        }
        try {
            JsonNode response = userManagementAgentClient.createUserGroupForUsername(
                    context.username(), request, BotAgentToolRegistry.locale(context));
            return BotAgentJsonSupport.createdUserGroupText(response);
        } catch (Exception ex) {
            log.warn("create_user_group failed for {}: {}", context.username(), ex.getMessage());
            return "Could not create user group: " + ex.getMessage()
                    + ". Ensure ldms-user-management is running and reachable.";
        }
    }
}
