package projectlx.messaging.inbound.business.logic.support.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentJsonSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.clients.UserManagementAgentClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddUsersToUserGroupTool implements BotAgentTool {

    private final UserManagementAgentClient userManagementAgentClient;
    private final ObjectMapper objectMapper;

    public AddUsersToUserGroupTool(UserManagementAgentClient userManagementAgentClient, ObjectMapper objectMapper) {
        this.userManagementAgentClient = userManagementAgentClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "add_users_to_user_group";
    }

    @Override
    public String description() {
        return "Add one or more users to a user group. Use list_user_groups and list_org_users first to resolve IDs.";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "userGroupId", BotAgentToolRegistry.longParam("Target user group ID from list_user_groups.", true),
                "userIds", BotAgentToolRegistry.stringParam(
                        "Comma-separated user IDs from list_org_users (e.g. 12,45,78).", true));
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        long userGroupId = BotAgentToolRegistry.readLong(arguments, "userGroupId");
        List<Long> userIds = parseUserIds(BotAgentToolRegistry.readString(arguments, "userIds"), arguments.get("userIds"));
        if (userGroupId <= 0) {
            return "userGroupId is required — call list_user_groups to find the group ID.";
        }
        if (userIds.isEmpty()) {
            return "At least one userId is required — call list_org_users to find user IDs.";
        }
        ObjectNode request = objectMapper.createObjectNode();
        request.put("userGroupId", userGroupId);
        ArrayNode ids = request.putArray("userIds");
        userIds.forEach(ids::add);
        JsonNode response = userManagementAgentClient.addUsersToUserGroupForUsername(
                context.username(), request, BotAgentToolRegistry.locale(context));
        return BotAgentJsonSupport.addUsersToGroupText(response, userGroupId, userIds.size());
    }

    private static List<Long> parseUserIds(String csv, Object rawValue) {
        List<Long> ids = new ArrayList<>();
        if (rawValue instanceof List<?> list) {
            for (Object item : list) {
                addParsedId(ids, item);
            }
            if (!ids.isEmpty()) {
                return ids;
            }
        }
        if (csv == null || csv.isBlank()) {
            return ids;
        }
        for (String part : csv.split("[,;\\s]+")) {
            addParsedId(ids, part);
        }
        return ids;
    }

    private static void addParsedId(List<Long> ids, Object value) {
        if (value == null) {
            return;
        }
        try {
            long id = Long.parseLong(String.valueOf(value).trim());
            if (id > 0 && !ids.contains(id)) {
                ids.add(id);
            }
        } catch (NumberFormatException ignored) {
            // skip invalid tokens
        }
    }
}
