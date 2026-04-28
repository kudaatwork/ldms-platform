package projectlx.user.management.service.rest.backoffice;

import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserTypeServiceProcessor;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.utils.requests.EditUserTypeRequest;
import projectlx.user.management.utils.requests.UserTypeMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserTypeResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/backoffice/user-type")
@Tag(name = "User Type Backoffice Resource", description = "Operations related to managing user types.")
@RequiredArgsConstructor
public class UserTypeBackofficeResource {

    private final UserTypeServiceProcessor userTypeServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserTypeBackofficeResource.class);

    @Auditable(action = "CREATE_USER_TYPE")
    @PostMapping("/create")
    @Operation(summary = "Create a new user type", description = "Creates a new user type and returns the created" +
            " user Type details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User type created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public UserTypeResponse create(@Valid @RequestBody final CreateUserTypeRequest createUserAddressRequest,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userTypeServiceProcessor.create(createUserAddressRequest, locale, "BACKOFFICE");
    }

    @Auditable(action = "UPDATE_USER_TYPE")
    @PutMapping("/update")
    @Operation(summary = "Update user type details", description = "Updates an existing user types details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User type updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public UserTypeResponse update(@Valid @RequestBody final EditUserTypeRequest editUserTypeRequest,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userTypeServiceProcessor.update(editUserTypeRequest, "BACKOFFICE", locale);
    }

    @Auditable(action = "FIND_USER_TYPE_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user type by ID", description = "Retrieves a user type by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User type found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public UserTypeResponse findById(@PathVariable("id") final Long id,
                                     @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                     @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                             defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userTypeServiceProcessor.findById(id, locale, "BACKOFFICE");
    }

    @Auditable(action = "DELETE_USER_TYPE")
    @Operation(summary = "Delete a user type by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public UserTypeResponse delete(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userTypeServiceProcessor.delete(id, locale, "BACKOFFICE");
    }

    @Auditable(action = "FIND_ALL_USER_TYPES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all user types", description = "Retrieves a list of all user types")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User types retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public UserTypeResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                           @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                   defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userTypeServiceProcessor.findAllAsList("BACKOFFICE", locale);
    }

    @Auditable(action = "FIND_USER_TYPES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find user types by multiple filters",
            description = "Retrieves a list of user type that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User type found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public UserTypeResponse findByMultipleFilters(@Valid @RequestBody UserTypeMultipleFiltersRequest
                                                          userTypeMultipleFiltersRequest,
                                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                          defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userTypeServiceProcessor.findByMultipleFilters(userTypeMultipleFiltersRequest, "BACKOFFICE", locale);
    }

    @Auditable(action = "EXPORT_USER_TYPES")
    @PostMapping("/export")
    @Operation(summary = "Export user types", 
            description = "Exports user types based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User types exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportUserTypes(@RequestBody UserTypeMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export user types in {} format with filters: {}", format, filters);

            switch (format.toLowerCase()) {
                case "csv":
                    data = userTypeServiceProcessor.exportToCsv(filters, "BACKOFFICE", locale);
                    contentType = "text/csv";
                    filename = "user-types.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = userTypeServiceProcessor.exportToExcel(filters, "BACKOFFICE", locale);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "user-types.xlsx";
                    break;

                case "pdf":
                    data = userTypeServiceProcessor.exportToPdf(filters, "BACKOFFICE", locale);
                    contentType = "application/pdf";
                    filename = "user-types.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported user types in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export user types: " + e.getMessage();
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

    @Auditable(action = "IMPORT_USER_TYPES_FROM_CSV")
    @PostMapping("/import/csv")
    @Operation(summary = "Import user types from CSV file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User types imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file")
    })
    public ResponseEntity<ImportSummary> importUserTypesFromCsv(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            logger.info("Incoming request to import user types from CSV file: {}", file.getOriginalFilename());

            ImportSummary summary = userTypeServiceProcessor.importUserTypesFromCsv(inputStream);

            logger.info("Successfully imported user types from CSV. Summary: {}", summary);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            logger.error("Error importing user types from CSV: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
