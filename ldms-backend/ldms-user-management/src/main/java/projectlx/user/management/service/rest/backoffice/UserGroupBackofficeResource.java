package projectlx.user.management.service.rest.backoffice;

import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserGroupServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.utils.requests.EditUserGroupRequest;
import projectlx.user.management.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserGroupResponse;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/backoffice/user-group")
@Tag(name = "User Group Backoffice Resource", description = "Operations related to managing user groups")
@RequiredArgsConstructor
public class UserGroupBackofficeResource {

    private final UserGroupServiceProcessor userGroupServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserGroupBackofficeResource.class);

    @Auditable(action = "CREATE_USER_GROUP") // This method call will be logged by the Aspect
    @PostMapping("/create")
    @Operation(summary = "Create a new user group", description = "Creates a new user group and returns the created" +
            " user group details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User group created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserGroupResponse create(@Valid @RequestBody final CreateUserGroupRequest createUserGroupRequest,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userGroupServiceProcessor.create(createUserGroupRequest, locale, "BACKOFFICE");
    }

    @Auditable(action = "UPDATE_USER_GROUP")
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
        return userGroupServiceProcessor.update(editUserGroupRequest, "BACKOFFICE", locale);
    }

    @Auditable(action = "FIND_USER_GROUP_BY_ID")
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
        return userGroupServiceProcessor.findById(id, locale, "BACKOFFICE");
    }

    @Auditable(action = "DELETE_USER_GROUP")
    @Operation(summary = "Delete a user group by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserGroupResponse delete(@PathVariable("id") final Long id,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userGroupServiceProcessor.delete(id, locale, "BACKOFFICE");
    }

    @Auditable(action = "FIND_ALL_USER_GROUPS_BY_LIST")
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
        return userGroupServiceProcessor.findAllAsList("BACKOFFICE", locale);
    }

    @Auditable(action = "FIND_ALL_USER_GROUPS_BY_MULTIPLE_FILTERS")
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
        return userGroupServiceProcessor.findByMultipleFilters(userGroupMultipleFiltersRequest, "BACKOFFICE", locale);
    }

    @Auditable(action = "ASSIGN_USER_ROLES_TO_USER_GROUP")
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
        return userGroupServiceProcessor.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest, locale,
                "BACKOFFICE");
    }

    @Auditable(action = "REMOVE_USER_ROLES_TO_USER_GROUP")
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
        return userGroupServiceProcessor.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale,
                "BACKOFFICE");
    }

    @Auditable(action = "ADD_USER_GROUP_TO_USER")
    @PostMapping(value = "/add-user-group-to-user")
    @Operation(summary = "Add user group to user", description = "Adds user group to a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User group added to user successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while user group to user")
    })
    public UserGroupResponse addUserGroupToUser(@Valid @RequestBody final AddUserToUserGroupRequest
                                                                  addUserToUserGroupRequest,
                                                          @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                          @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                  defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userGroupServiceProcessor.addUserGroupToUser(addUserToUserGroupRequest, locale,
                "BACKOFFICE");
    }

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
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export user groups in {} format with filters: {}", format, filters);

            switch (format.toLowerCase()) {
                case "csv":
                    data = userGroupServiceProcessor.exportToCsv(filters, "BACKOFFICE", locale);
                    contentType = "text/csv";
                    filename = "user-groups.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = userGroupServiceProcessor.exportToExcel(filters, "BACKOFFICE", locale);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "user-groups.xlsx";
                    break;

                case "pdf":
                    data = userGroupServiceProcessor.exportToPdf(filters, "BACKOFFICE", locale);
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
