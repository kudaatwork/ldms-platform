package projectlx.user.management.service.service.rest.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.service.processor.api.UserServiceProcessor;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.requests.CreateUserRequest;
import projectlx.user.management.service.utils.requests.EditUserRequest;
import projectlx.user.management.service.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.service.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserResponse;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/system/user")
@Tag(name = "User System Resource", description = "Operations related to managing users")
@RequiredArgsConstructor
public class UserSystemResource {

    private final UserServiceProcessor userServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserSystemResource.class);

    @Auditable(action = "CREATE_USER")
    @PostMapping("/create")
    @Operation(summary = "Create a new user", description = "Creates a new user and returns the created user details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserResponse createUser(@Valid @ModelAttribute final CreateUserRequest createUserRequest,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                 defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userServiceProcessor.create(createUserRequest, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_USER")
    @PutMapping("/update")
    @Operation(summary = "Update user details", description = "Updates an existing user's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserResponse update(@Valid @ModelAttribute final EditUserRequest editUserRequest,
                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userServiceProcessor.update(editUserRequest, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_USER_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user by ID", description = "Retrieves a user by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserResponse findById(@PathVariable("id") final Long id,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                            defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_USER_BY_USERNAME")
    @GetMapping(value = "/find-by-username/{username}")
    @Operation(summary = "Find user by username", description = "Retrieves a user by their unique username.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserResponse findByUsername(@PathVariable("username") final String username,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                         defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userServiceProcessor.findByUsername(username, locale);
    }
    
    @Auditable(action = "FIND_USER_BY_PHONE_NUMBER_OR_EMAIL")
    @GetMapping(value = "/find-by-phone-number-or-email/{phoneNumberOrEmail}")
    @Operation(summary = "Find user by phone number or email", description = "Retrieves a user by their phone number or email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Phone number or email supplied invalid")
    })
    public UserResponse findByPhoneNumberOrEmail(@PathVariable("phoneNumberOrEmail") final String phoneNumberOrEmail,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                         defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userServiceProcessor.findByPhoneNumberOrEmail(phoneNumberOrEmail, locale);
    }

    @Auditable(action = "DELETE_USER")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete by id", description = "Deletes a user by their unique id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserResponse delete(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_USERS_AS_A_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all users", description = "Retrieves a list of all users.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public UserResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                           @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                   defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return userServiceProcessor.findAllAsList(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_USERS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find users by multiple filters",
            description = "Retrieves a list of users that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public UserResponse findByMultipleFilters(@Valid @RequestBody UsersMultipleFiltersRequest usersMultipleFiltersRequest,
                                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                         defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userServiceProcessor.findByMultipleFilters(usersMultipleFiltersRequest, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_USERS_BY_ORGANIZATION")
    @GetMapping(value = "/find-by-organization-id/{organizationId}")
    @Operation(summary = "Find users by organization ID",
            description = "Retrieves a list of users that belong to the specified organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid organization ID")
    })
    public UserResponse findByOrganizationId(@PathVariable("organizationId") final Long organizationId,
                                             @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                             @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                     defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("Incoming request to find users by organization ID: {}", organizationId);
        UserResponse response = userServiceProcessor.findByOrganizationId(organizationId, locale, "SYSTEM");
        logger.info("Outgoing response after finding users by organization ID: {}. Status Code: {}. Message: {}", 
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Auditable(action = "FIND_USERS_BY_BRANCH")
    @GetMapping(value = "/find-by-branch-id/{branchId}")
    @Operation(summary = "Find users by branch ID",
            description = "Retrieves a list of users that belong to the specified branch.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid branch ID")
    })
    public UserResponse findByBranchId(@PathVariable("branchId") final Long branchId,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                               defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("Incoming request to find users by branch ID: {}", branchId);
        UserResponse response = userServiceProcessor.findByBranchId(branchId, locale, "SYSTEM");
        logger.info("Outgoing response after finding users by branch ID: {}. Status Code: {}. Message: {}", 
                response, response.getStatusCode(), response.getMessage());
        return response;
    }

    @Auditable(action = "EXPORT_USER_USERS")
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportUsers(@RequestBody UsersMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {

            switch (format.toLowerCase()) {

                case "csv":

                    data = userServiceProcessor.exportToCsv(filters, "SYSTEM", locale);
                    contentType = "text/csv";
                    filename = "users.csv";
                    break;

                case "excel":
                case "xlsx":

                    data = userServiceProcessor.exportToExcel(filters, "SYSTEM", locale);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "users.xlsx";
                    break;

                case "pdf":

                    data = userServiceProcessor.exportToPdf(filters, "SYSTEM", locale);
                    contentType = "application/pdf";
                    filename = "users.pdf";
                    break;

                default:

                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

        } catch (Exception e) {

            String errorMsg = "Failed to export users: " + e.getMessage();
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

    @Auditable(action = "IMPORT_USERS_FROM_CSV")
    @PostMapping("/import/csv")
    @Operation(summary = "Import users from CSV file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file")
    })
    public ResponseEntity<ImportSummary> importUsersFromCsv(@RequestParam("file") MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {

            ImportSummary summary = userServiceProcessor.importUsersFromCsv(inputStream);

            return ResponseEntity.ok(summary);

        } catch (IOException e) {

            logger.error("Error importing users from CSV: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Auditable(action = "IMPORT_USERS_FROM_EXCEL")
    @PostMapping("/import/excel")
    @Operation(summary = "Import users from Excel file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Excel file")
    })
    public ResponseEntity<ImportSummary> importUsersFromExcel(@RequestParam("file") MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {

            ImportSummary summary = userServiceProcessor.importUsersFromExcel(inputStream);
            return ResponseEntity.ok(summary);

        } catch (IOException e) {

            logger.error("Error importing users from Excel: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Auditable(action = "VERIFY_USER_EMAIL")
    @PostMapping(value = "/verify-email")
    @Operation(summary = "Verify user's email",
            description = "Verifies user's email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "404", description = "Email not found"),
            @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public UserResponse verifyEmail(@RequestParam String token, @RequestParam String email,
                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return userServiceProcessor.verifyEmail(email, token, locale, "SYSTEM");
    }

    @Auditable(action = "FORGOT_PASSWORD")
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset",
            description = "Sends a password reset link to the user's email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset email sent"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public UserResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
                                       @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                               defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return userServiceProcessor.forgotPassword(forgotPasswordRequest, locale);
    }

    @Auditable(action = "VALIDATE_RESET_TOKEN")
    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validate password reset token",
            description = "Validates if the password reset token is valid and not expired")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "400", description = "Token is invalid or expired")
    })
    public UserResponse validateResetToken(@RequestParam String token,
                                           @RequestParam String email,
                                           @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                           @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                   defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return userServiceProcessor.validateResetToken(token, email, locale);
    }

    @Auditable(action = "RESEND_VERIFICATION_LINK")
    @PostMapping("/resend-verification-link")
    @Operation(summary = "Resend verification link",
            description = "Resends a new verification link to the user's email if not yet verified.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification link sent successfully"),
            @ApiResponse(responseCode = "400", description = "Email already verified or invalid request"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserResponse resendVerificationLink(@RequestParam String email,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return userServiceProcessor.resendVerificationLink(email, locale, "SYSTEM");
    }
}
