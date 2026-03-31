package projectlx.user.management.service.service.rest.frontend;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.service.processor.api.UserGroupServiceProcessor;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.service.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.service.utils.requests.EditUserGroupRequest;
import projectlx.user.management.service.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.service.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserGroupResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/frontend/user-group")
@Tag(name = "User Group Frontend Resource", description = "Operations related to managing user groups")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserGroupFrontendResource {

    private final UserGroupServiceProcessor userGroupServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserGroupFrontendResource.class);

    @Auditable(action = "CREATE_USER_GROUP")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "CREATE_USER_GROUP.toString())")
    @PostMapping("/create")
    @Operation(summary = "Create a new user group", description = "Creates a new user group and returns the created" +
            " user group details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User group created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserGroupResponse create(@Valid @RequestBody final CreateUserGroupRequest createUserAddressRequest,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.create(createUserAddressRequest, locale, username);
    }

    @Auditable(action = "UPDATE_USER_GROUP")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "UPDATE_USER_GROUP.toString())")
    @PutMapping("/update")
    @Operation(summary = "Update user group details", description = "Updates an existing user group' details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User group updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserGroupResponse update(@Valid @RequestBody final EditUserGroupRequest editUserGroupRequest,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.update(editUserGroupRequest, username, locale);
    }

    @Auditable(action = "FIND_USER_GROUP_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "VIEW_USER_GROUP_BY_ID.toString())")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user group by ID", description = "Retrieves a user group by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User group found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserGroupResponse findById(@PathVariable("id") final Long id,
                                        @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                        @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_USER_GROUP")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "DELETE_USER_GROUP.toString())")
    @Operation(summary = "Delete a user group by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserGroupResponse delete(@PathVariable("id") final Long id,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_USER_GROUPS_BY_LIST")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "VIEW_ALL_USER_GROUPS_AS_A_LIST.toString())")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all user groups", description = "Retrieves a list of all user groups")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User groups retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public UserGroupResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.findAllAsList(username, locale);
    }

    @Auditable(action = "FIND_ALL_USER_GROUPS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "VIEW_ALL_USER_GROUPS_BY_MULTIPLE_FILTERS.toString())")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find user groups by multiple filters",
            description = "Retrieves a list of user group that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User group found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public UserGroupResponse findByMultipleFilters(@Valid @RequestBody UserGroupMultipleFiltersRequest
                                                           userGroupMultipleFiltersRequest,
                                                     @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                     @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                             defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.findByMultipleFilters(userGroupMultipleFiltersRequest, username, locale);
    }

    @Auditable(action = "ASSIGN_USER_ROLES_TO_USER_GROUP")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "ASSIGN_USER_ROLES_TO_USER_GROUP.toString())")
    @PostMapping(value = "/assign-user-roles-to-user-group")
    @Operation(summary = "Assign user roles to a user group", description = "Assigns user role(s) to a user group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User roles assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while assigning user roles to a user group")
    })
    public UserGroupResponse assignUserRolesToUserGroup(@Valid @RequestBody final AssignUserRoleToUserGroupRequest
                                                                assignUserRoleToUserGroupRequest,
                                                        @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                        @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest, locale, username);
    }

    @Auditable(action = "REMOVE_USER_ROLES_FROM_USER_GROUP")
    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles)." +
            "REMOVE_USER_ROLES_FROM_USER_GROUP.toString())")
    @PostMapping(value = "/remove-user-roles-from-user-group")
    @Operation(summary = "Remove user roles from a user group", description = "Removes user role(s) to a user group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User roles removed successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while removing user roles to a user group")
    })
    public UserGroupResponse removeUserRolesFromUserGroup(@Valid @RequestBody final RemoveUserRolesFromUserGroupRequest
                                                                  removeUserRolesFromUserGroupRequest,
                                                          @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                          @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                  defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userGroupServiceProcessor.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale, username);
    }

    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles).IMPORT_USER_GROUPS.toString())")
    @Auditable(action = "IMPORT_USER_GROUPS_FROM_CSV")
    @PostMapping("/import/csv")
    @Operation(summary = "Import user groups from CSV file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User groups imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file")
    })
    public ResponseEntity<ImportSummary> importUserGroupsFromCsv(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            logger.info("Incoming request to import user groups from CSV file: {}", file.getOriginalFilename());

            ImportSummary summary = userGroupServiceProcessor.importUserGroupsFromCsv(inputStream);

            logger.info("Successfully imported user groups from CSV. Summary: {}", summary);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("Error importing user groups from CSV: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole(T(projectlx.user.management.service.utils.security.UserGroupRoles).EXPORT_USER_GROUPS.toString())")
    @Auditable(action = "EXPORT_USER_GROUPS")
    @PostMapping("/export")
    @Operation(summary = "Export user groups", 
            description = "Exports user groups based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User groups exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportUserGroups(@RequestBody UserGroupMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export user groups in {} format with filters: {}", format, filters);

            switch (format.toLowerCase()) {
                case "csv":
                    data = userGroupServiceProcessor.exportToCsv(filters, username, locale);
                    contentType = "text/csv";
                    filename = "user-groups.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = userGroupServiceProcessor.exportToExcel(filters, username, locale);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "user-groups.xlsx";
                    break;

                case "pdf":
                    data = userGroupServiceProcessor.exportToPdf(filters, username, locale);
                    contentType = "application/pdf";
                    filename = "user-groups.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported user groups in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export user groups: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
