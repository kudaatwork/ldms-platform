package projectlx.user.management.service.rest.system;

import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserSecurityServiceProcessor;
import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.utils.requests.UserSecurityMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserSecurityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/system/user-security")
@Tag(name = "User Security System Resource", description = "Operations related to managing user Securities.")
@RequiredArgsConstructor
public class UserSecuritySystemResource {

    private final UserSecurityServiceProcessor userSecurityServiceProcessor;

    @Auditable(action = "CREATE_USER_SECURITY")
    @PostMapping("/create")
    @Operation(summary = "Create a new user security", description = "Creates a new user security and returns the created" +
            " user Security details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User security created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserSecurityResponse create(@Valid @RequestBody final CreateUserSecurityRequest createUserAddressRequest,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userSecurityServiceProcessor.create(createUserAddressRequest, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_USER_SECURITY")
    @PutMapping("/update")
    @Operation(summary = "Update user security details", description = "Updates an existing user Security' details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User security updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserSecurityResponse update(@Valid @RequestBody final EditUserSecurityRequest editUserSecurityRequest,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userSecurityServiceProcessor.update(editUserSecurityRequest, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_USER_SECURITY_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user security by ID", description = "Retrieves a user security by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User security found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserSecurityResponse findById(@PathVariable("id") final Long id,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userSecurityServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_USER_SECURITY")
    @Operation(summary = "Delete a user security by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserSecurityResponse delete(@PathVariable("id") final Long id,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userSecurityServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_USER_SECURITIES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all user securities", description = "Retrieves a list of all user securities")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User securities retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public UserSecurityResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userSecurityServiceProcessor.findAllAsList("SYSTEM", locale);
    }

    @Auditable(action = "FIND_USER_SECURITIES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find user securities by multiple filters",
            description = "Retrieves a list of user Security that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User security found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public UserSecurityResponse findByMultipleFilters(@Valid @RequestBody UserSecurityMultipleFiltersRequest
                                                           userSecurityMultipleFiltersRequest,
                                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userSecurityServiceProcessor.findByMultipleFilters(userSecurityMultipleFiltersRequest, "SYSTEM", locale);
    }
}
