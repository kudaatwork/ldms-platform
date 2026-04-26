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
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserAccountServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.utils.requests.EditUserAccountRequest;
import projectlx.user.management.utils.requests.UserAccountMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserAccountResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/frontend/user-account")
@Tag(name = "User Accounts Frontend Resource", description = "Operations related to managing user accounts")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserAccountFrontendResource {

    private final UserAccountServiceProcessor userAccountServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserAccountFrontendResource.class);

    @Auditable(action = "CREATE_USER_ACCOUNT")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserAccountRoles)." +
            "CREATE_USER_ACCOUNT.toString())")
    @PostMapping("/create")
    @Operation(summary = "Create a new user account", description = "Creates a new user account and returns the created" +
            " user account details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserAccountResponse create(@Valid @RequestBody final CreateUserAccountRequest createUserAccountRequest,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountServiceProcessor.create(createUserAccountRequest, locale, username);
    }

    @Auditable(action = "UPDATE_USER_ACCOUNT")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserAccountRoles)." +
            "UPDATE_USER_ACCOUNT.toString())")
    @PutMapping("/update")
    @Operation(summary = "Update user account details", description = "Updates an existing user account' details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User account updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserAccountResponse update(@Valid @RequestBody final EditUserAccountRequest editUserAccountRequest,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountServiceProcessor.update(editUserAccountRequest, username, locale);
    }

    @Auditable(action = "FIND_USER_ACCOUNT_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserAccountRoles)." +
            "VIEW_USER_ACCOUNT_BY_ID.toString())")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user account by ID", description = "Retrieves a user account by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User account found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserAccountResponse findById(@PathVariable("id") final Long id,
                                        @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                        @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_USER_ACCOUNT")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserAccountRoles)." +
            "DELETE_USER_ACCOUNT.toString())")
    @Operation(summary = "Delete a user account by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserAccountResponse delete(@PathVariable("id") final Long id,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_USER_ACCOUNTS_AS_A_LIST")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserAccountRoles)." +
            "VIEW_ALL_USER_ACCOUNTS_AS_A_LIST.toString())")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all user accounts", description = "Retrieves a list of all user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User accounts retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public UserAccountResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountServiceProcessor.findAllAsList(username, locale);
    }

    @Auditable(action = "FIND_USER_ACCOUNTS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserAccountRoles)." +
            "VIEW_ALL_USER_ACCOUNTS_BY_MULTIPLE_FILTERS.toString())")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find user accounts by multiple filters",
            description = "Retrieves a list of user accounts that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User accounts found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public UserAccountResponse findByMultipleFilters(@Valid @RequestBody UserAccountMultipleFiltersRequest
                                                             userAccountMultipleFiltersRequest,
                                                     @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                     @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                             defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountServiceProcessor.findByMultipleFilters(userAccountMultipleFiltersRequest, username, locale);
    }

    @Auditable(action = "IMPORT_USER_ACCOUNTS_FROM_CSV")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoles).IMPORT_USERS.toString())")
    @PostMapping("/import/csv")
    @Operation(summary = "Import user accounts from CSV file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User accounts imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file")
    })
    public ResponseEntity<ImportSummary> importUserAccountsFromCsv(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            logger.info("Incoming request to import user accounts from CSV file: {}", file.getOriginalFilename());

            ImportSummary summary = userAccountServiceProcessor.importUserAccountsFromCsv(inputStream);

            logger.info("Successfully imported user accounts from CSV. Summary: {}", summary);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("Error importing user accounts from CSV: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Auditable(action = "IMPORT_USER_ACCOUNTS_FROM_EXCEL")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoles).IMPORT_USERS.toString())")
    @PostMapping("/import/excel")
    @Operation(summary = "Import user accounts from Excel file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User accounts imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Excel file")
    })
    public ResponseEntity<ImportSummary> importUserAccountsFromExcel(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            logger.info("Incoming request to import user accounts from Excel file: {}", file.getOriginalFilename());

            ImportSummary summary = userAccountServiceProcessor.importUserAccountsFromExcel(inputStream);

            logger.info("Successfully imported user accounts from Excel. Summary: {}", summary);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("Error importing user accounts from Excel: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Auditable(action = "EXPORT_USER_ACCOUNTS")
    @PreAuthorize("hasRole(T(projectlx.user.management.utils.security.UserRoles).EXPORT_USERS.toString())")
    @PostMapping("/export")
    @Operation(summary = "Export user accounts", 
            description = "Exports user accounts based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User accounts exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportUserAccounts(@RequestBody UserAccountMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export user accounts in {} format with filters: {}", format, filters);

            switch (format.toLowerCase()) {
                case "csv":
                    data = userAccountServiceProcessor.exportToCsv(filters, username, locale);
                    contentType = "text/csv";
                    filename = "user-accounts.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = userAccountServiceProcessor.exportToExcel(filters, username, locale);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "user-accounts.xlsx";
                    break;

                case "pdf":
                    data = userAccountServiceProcessor.exportToPdf(filters, username, locale);
                    contentType = "application/pdf";
                    filename = "user-accounts.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported user accounts in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export user accounts: " + e.getMessage();
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
