package projectlx.messaging.inbound.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface UserManagementAgentClient {

    @PostMapping("/ldms-user-management/v1/system/agent/user-group/by-username/{username}/create")
    JsonNode createUserGroupForUsername(
            @PathVariable("username") String username,
            @RequestBody JsonNode request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-user-management/v1/system/agent/user-group/by-username/{username}/list")
    JsonNode listUserGroupsForUsername(
            @PathVariable("username") String username,
            @RequestBody JsonNode request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-user-management/v1/system/agent/user-group/by-username/{username}/add-users")
    JsonNode addUsersToUserGroupForUsername(
            @PathVariable("username") String username,
            @RequestBody JsonNode request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-user-management/v1/system/agent/user/by-username/{username}/list")
    JsonNode listUsersForUsername(
            @PathVariable("username") String username,
            @RequestBody JsonNode request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
