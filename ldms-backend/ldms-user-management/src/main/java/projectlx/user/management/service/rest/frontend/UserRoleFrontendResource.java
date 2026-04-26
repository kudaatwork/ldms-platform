package projectlx.user.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserRoleServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.utils.requests.EditUserRoleRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserRoleResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/frontend/user-role")
@Tag(name = "User Role Frontend Resource", description = "Operations related to managing user roles.")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserRoleFrontendResource {

    private final UserRoleServiceProcessor userRoleServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserRoleFrontendResource.class);

    @Auditable(action = "CREATE_USER_ROLE")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoleRoles).CREATE_USER_ROLE.toString())")
    @PostMapping("/create")
    @Operation(summary = "Create a new user Role", description = "Creates a new user role and returns the created" +
            " user Role details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User role created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserRoleResponse create(@Valid @RequestBody final CreateUserRoleRequest createUserAddressRequest,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRoleServiceProcessor.create(createUserAddressRequest, locale, username);
    }

    @Auditable(action = "UPDATE_USER_ROLE")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoleRoles).UPDATE_USER_ROLE.toString())")
    @PutMapping("/update")
    @Operation(summary = "Update user role details", description = "Updates an existing user roles details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User role updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserRoleResponse update(@Valid @RequestBody final EditUserRoleRequest editUserRoleRequest,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRoleServiceProcessor.update(editUserRoleRequest, username, locale);
    }

    @Auditable(action = "FIND_USER_ROLE_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoleRoles).VIEW_USER_ROLE_BY_ID.toString())")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user role by ID", description = "Retrieves a user role by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User role found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserRoleResponse findById(@PathVariable("id") final Long id,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRoleServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_USER_ROLE")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoleRoles).DELETE_USER_ROLE.toString())")
    @Operation(summary = "Delete a user role by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserRoleResponse delete(@PathVariable("id") final Long id,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRoleServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_USER_ROLES_BY_LIST")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoleRoles).VIEW_ALL_USER_ROLES_AS_A_LIST.toString())")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all user roles", description = "Retrieves a list of all user roles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User roles retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public UserRoleResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRoleServiceProcessor.findAllAsList(username, locale);
    }

    @Auditable(action = "FIND_ALL_USER_ROLES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoleRoles).VIEW_ALL_USER_ROLES_BY_MULTIPLE_FILTERS.toString())")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find user roles by multiple filters",
            description = "Retrieves a list of user role that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User role found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public UserRoleResponse findByMultipleFilters(@Valid @RequestBody UserRoleMultipleFiltersRequest
                                                           userRoleMultipleFiltersRequest,
                                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRoleServiceProcessor.findByMultipleFilters(userRoleMultipleFiltersRequest, username, locale);
    }

    @Auditable(action = "IMPORT_USER_ROLES_FROM_CSV")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoleRoles).IMPORT_USER_ROLES.toString())")
    @PostMapping("/import/csv")
    @Operation(summary = "Import user roles from CSV file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User roles imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file")
    })
    public ResponseEntity<ImportSummary> importUserRolesFromCsv(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            logger.info("Incoming request to import user roles from CSV file: {}", file.getOriginalFilename());

            ImportSummary summary = userRoleServiceProcessor.importUserRolesFromCsv(inputStream);

            logger.info("Successfully imported user roles from CSV. Summary: {}", summary);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("Error importing user roles from CSV: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
