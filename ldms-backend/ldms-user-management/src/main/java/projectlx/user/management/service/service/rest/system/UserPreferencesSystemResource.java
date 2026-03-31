package projectlx.user.management.service.service.rest.system;

import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.service.processor.api.UserPreferencesServiceProcessor;
import projectlx.user.management.service.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.UserPreferencesMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserPreferencesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
@RequestMapping("/api/v1/system/user-preferences")
@Tag(name = "User Preferences System Resource", description = "Operations related to managing user preferences")
@RequiredArgsConstructor
public class UserPreferencesSystemResource {

    private final UserPreferencesServiceProcessor userPreferencesServiceProcessor;

    @Auditable(action = "CREATE_USER_PREFERENCES")
    @PostMapping("/create")
    @Operation(summary = "Create a new user preferences", description = "Creates a new user preferences and returns the created" +
            " user preferences details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User preferences created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserPreferencesResponse create(@Valid @RequestBody final CreateUserPreferencesRequest createUserPreferencesRequest,
                                          @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userPreferencesServiceProcessor.create(createUserPreferencesRequest, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_USER_PREFERENCES")
    @PutMapping("/update")
    @Operation(summary = "Update user preferences details", description = "Updates an existing user preferences' details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User preferences updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserPreferencesResponse update(@Valid @RequestBody final EditUserPreferencesRequest editUserPreferencesRequest,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userPreferencesServiceProcessor.update(editUserPreferencesRequest, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_USER_PREFERENCES_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user preferences by ID", description = "Retrieves a user preferences by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User preferences found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserPreferencesResponse findById(@PathVariable("id") final Long id,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userPreferencesServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_USER_PREFERENCES")
    @Operation(summary = "Delete a user preferences by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserPreferencesResponse delete(@PathVariable("id") final Long id,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userPreferencesServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_USER_PREFERENCES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all user preferences", description = "Retrieves a list of all user preferences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User preferences retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public UserPreferencesResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userPreferencesServiceProcessor.findAllAsList("SYSTEM", locale);
    }

    @Auditable(action = "FIND_ALL_USER_PREFERENCES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find user preferences by multiple filters",
            description = "Retrieves a list of user preferences that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User preferences found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public UserPreferencesResponse findByMultipleFilters(@Valid @RequestBody UserPreferencesMultipleFiltersRequest
                                                           userPreferencesMultipleFiltersRequest,
                                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userPreferencesServiceProcessor.findByMultipleFilters(userPreferencesMultipleFiltersRequest, "SYSTEM", locale);
    }
}
