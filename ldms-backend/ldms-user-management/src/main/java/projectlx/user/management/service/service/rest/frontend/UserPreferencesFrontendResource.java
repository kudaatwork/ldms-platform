package projectlx.user.management.service.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.service.processor.api.UserPreferencesServiceProcessor;
import projectlx.user.management.service.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.UserPreferencesMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserPreferencesResponse;

import java.util.Locale;
import org.springframework.security.core.context.SecurityContextHolder;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/frontend/user-preferences")
@Tag(name = "User Preferences Frontend Resource", description = "Operations related to managing user preferences")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserPreferencesFrontendResource {

    private final UserPreferencesServiceProcessor userPreferencesServiceProcessor;

    @Auditable(action = "CREATE_USER_PREFERENCES")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserPreferencesRoles)." +
            "CREATE_USER_PREFERENCES.toString())")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userPreferencesServiceProcessor.create(createUserPreferencesRequest, locale, username);
    }

    @Auditable(action = "UPDATE_USER_PREFERENCES")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserPreferencesRoles)." +
            "UPDATE_USER_PREFERENCES.toString())")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userPreferencesServiceProcessor.update(editUserPreferencesRequest, username, locale);
    }

    @Auditable(action = "FIND_USER_PREFERENCES_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserPreferencesRoles)." +
            "VIEW_USER_PREFERENCES_BY_ID.toString())")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userPreferencesServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_USER_PREFERENCES")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserPreferencesRoles)." +
            "DELETE_USER_PREFERENCES.toString())")
    @Operation(summary = "Delete a user preferences by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserPreferencesResponse delete(@PathVariable("id") final Long id,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userPreferencesServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_USER_PREFERENCES_BY_LIST")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserPreferencesRoles)." +
            "VIEW_ALL_USER_PREFERENCES_AS_A_LIST.toString())")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userPreferencesServiceProcessor.findAllAsList(username, locale);
    }

    @Auditable(action = "FIND_ALL_USER_PREFERENCES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserPreferencesRoles)." +
            "VIEW_ALL_USER_PREFERENCES_BY_MULTIPLE_FILTERS.toString())")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userPreferencesServiceProcessor.findByMultipleFilters(userPreferencesMultipleFiltersRequest, username, locale);
    }
}
