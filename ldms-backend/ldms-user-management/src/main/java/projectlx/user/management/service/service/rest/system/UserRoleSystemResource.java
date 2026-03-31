package projectlx.user.management.service.service.rest.system;

import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.service.processor.api.UserRoleServiceProcessor;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.service.utils.requests.EditUserRoleRequest;
import projectlx.user.management.service.utils.requests.UserRoleMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserRoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/system/user-role")
@Tag(name = "User Role System Resource", description = "Operations related to managing user roles.")
@RequiredArgsConstructor
public class UserRoleSystemResource {

    private final UserRoleServiceProcessor userRoleServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserRoleSystemResource.class);

    @Auditable(action = "CREATE_USER_ROLE")
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
        return userRoleServiceProcessor.create(createUserAddressRequest, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_USER_ROLE")
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
        return userRoleServiceProcessor.update(editUserRoleRequest, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_USER_ROLE_BY_ID")
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
        return userRoleServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_USER_ROLE")
    @Operation(summary = "Delete a user role by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserRoleResponse delete(@PathVariable("id") final Long id,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userRoleServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_USER_ROLES_BY_LIST")
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
        return userRoleServiceProcessor.findAllAsList("SYSTEM", locale);
    }

    @Auditable(action = "FIND_ALL_USER_ROLES_BY_MULTIPLE_FILTERS")
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
        return userRoleServiceProcessor.findByMultipleFilters(userRoleMultipleFiltersRequest, "SYSTEM", locale);
    }

    @Auditable(action = "EXPORT_USER_ROLES")
    @PostMapping("/export")
    @Operation(summary = "Export user roles", 
            description = "Exports user roles based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User roles exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportUserRoles(@RequestBody UserRoleMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export user roles in {} format with filters: {}", format, filters);

            switch (format.toLowerCase()) {
                case "csv":
                    data = userRoleServiceProcessor.exportToCsv(filters, "SYSTEM", locale);
                    contentType = "text/csv";
                    filename = "user-roles.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = userRoleServiceProcessor.exportToExcel(filters, "SYSTEM", locale);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "user-roles.xlsx";
                    break;

                case "pdf":
                    data = userRoleServiceProcessor.exportToPdf(filters, "SYSTEM", locale);
                    contentType = "application/pdf";
                    filename = "user-roles.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported user roles in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export user roles: " + e.getMessage();
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
