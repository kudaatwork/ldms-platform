package projectlx.user.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserGroupServiceProcessor;
import projectlx.user.management.utils.requests.AddUsersToUserGroupRequest;
import projectlx.user.management.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserGroupResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/system/agent/user-group")
@Tag(name = "User Group Agent System Resource", description = "Lexi agent — user group actions scoped to portal user")
@RequiredArgsConstructor
public class UserGroupAgentSystemResource {

    private final UserGroupServiceProcessor userGroupServiceProcessor;

    @Auditable(action = "AGENT_CREATE_USER_GROUP_FOR_USERNAME")
    @PostMapping("/by-username/{username}/create")
    @Operation(summary = "Create a user group for the portal user's organisation workspace")
    public ResponseEntity<UserGroupResponse> createForUsername(
            @PathVariable String username,
            @Valid @RequestBody CreateUserGroupRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        UserGroupResponse response = userGroupServiceProcessor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "AGENT_LIST_USER_GROUPS_FOR_USERNAME")
    @PostMapping("/by-username/{username}/list")
    @Operation(summary = "List user groups visible to the portal user's organisation")
    public ResponseEntity<UserGroupResponse> listForUsername(
            @PathVariable String username,
            @RequestBody(required = false) UserGroupMultipleFiltersRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        UserGroupMultipleFiltersRequest filters = request == null ? new UserGroupMultipleFiltersRequest() : request;
        if (filters.getPage() < 0) {
            filters.setPage(0);
        }
        if (filters.getSize() <= 0) {
            filters.setSize(20);
        }
        UserGroupResponse response = userGroupServiceProcessor.findByMultipleFilters(filters, username, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "AGENT_ADD_USERS_TO_USER_GROUP_FOR_USERNAME")
    @PostMapping("/by-username/{username}/add-users")
    @Operation(summary = "Add users to a user group in the portal user's organisation workspace")
    public ResponseEntity<UserGroupResponse> addUsersForUsername(
            @PathVariable String username,
            @Valid @RequestBody AddUsersToUserGroupRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        UserGroupResponse response = userGroupServiceProcessor.addUsersToUserGroup(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
