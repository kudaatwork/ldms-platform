package projectlx.user.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import projectlx.user.management.service.processor.api.UserServiceProcessor;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/system/agent/user")
@Tag(name = "User Agent System Resource", description = "Lexi agent — user lookup scoped to portal user")
@RequiredArgsConstructor
public class UserAgentSystemResource {

    private final UserServiceProcessor userServiceProcessor;

    @Auditable(action = "AGENT_LIST_USERS_FOR_USERNAME")
    @PostMapping("/by-username/{username}/list")
    @Operation(summary = "List users in the portal user's organisation workspace")
    public ResponseEntity<UserResponse> listForUsername(
            @PathVariable String username,
            @RequestBody(required = false) UsersMultipleFiltersRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        UsersMultipleFiltersRequest filters = request == null ? new UsersMultipleFiltersRequest() : request;
        if (filters.getPage() < 0) {
            filters.setPage(0);
        }
        if (filters.getSize() <= 0) {
            filters.setSize(20);
        }
        UserResponse response = userServiceProcessor.findByMultipleFilters(filters, username, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
