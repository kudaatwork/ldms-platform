package projectlx.inventory.management.clients;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import java.util.Locale;

public interface UserManagementServiceClient {

    @GetMapping("/ldms-user-management/v1/system/user/session-profile-by-username/{username}")
    UserResponse findSessionProfileByUsername(@PathVariable("username") String username);

    @Auditable(action = "FIND_USER_BY_PHONE_NUMBER_OR_EMAIL")
    @GetMapping(value = "/ldms-user-management/v1/system/user/find-by-phone-number-or-email/{phoneNumberOrEmail}")
    @Operation(summary = "Find user by phone number or email", description = "Retrieves a user by their phone number or email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Phone number or email supplied invalid")
    })
    UserResponse findByPhoneNumberOrEmail(@PathVariable("phoneNumberOrEmail") String phoneNumberOrEmail,
                                          @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                  defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);

    @GetMapping(value = "/ldms-user-management/v1/system/user/find-by-id/{id}")
    @Operation(summary = "Find user by ID", description = "Retrieves a user by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })

    UserResponse findById(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                         defaultValue = Constants.DEFAULT_LOCALE) final Locale locale);
}
