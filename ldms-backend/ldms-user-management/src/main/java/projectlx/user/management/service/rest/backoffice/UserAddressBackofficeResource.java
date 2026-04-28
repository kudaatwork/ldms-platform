package projectlx.user.management.service.rest.backoffice;

import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.UserAddressServiceProcessor;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.utils.responses.AddressResponse;
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
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/backoffice/user-address")
@Tag(name = "User Address Backoffice Resource", description = "Operations related to managing user addresses")
@RequiredArgsConstructor
public class UserAddressBackofficeResource {

    private final UserAddressServiceProcessor userAddressServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(UserAddressBackofficeResource.class);

    @Auditable(action = "CREATE_USER_ADDRESS")
    @PostMapping("/create")
    @Operation(summary = "Create a new user address", description = "Creates a new user address and returns the created" +
            " user address details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User address created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public AddressResponse create(@Valid @RequestBody final CreateAddressRequest createAddressRequest,
                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userAddressServiceProcessor.create(createAddressRequest, locale, "BACKOFFICE");
    }

    @Auditable(action = "UPDATE_USER_ADDRESS")
    @PutMapping("/update")
    @Operation(summary = "Update user address details", description = "Updates an existing user address' details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User address updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public AddressResponse update(@Valid @RequestBody final EditAddressRequest editAddressRequest,
                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return userAddressServiceProcessor.update(editAddressRequest, "BACKOFFICE", locale);
    }

    @Auditable(action = "FIND_USER_ADDRESS_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find user address by ID", description = "Retrieves a user address by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User address found successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User id supplied invalid")
    })
    public AddressResponse findById(@PathVariable("id") final Long id,
                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                         defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userAddressServiceProcessor.findById(id, locale, "BACKOFFICE");
    }

    @Auditable(action = "DELETE_USER_ADDRESS")
    @Operation(summary = "Delete a user address by id")
    @DeleteMapping(value = "/delete-by-id/{id}")
    public AddressResponse delete(@PathVariable("id") final Long id,
                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userAddressServiceProcessor.delete(id, locale, "BACKOFFICE");
    }

    @Auditable(action = "FIND_ALL_USER_ADDRESSES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all user addresses", description = "Retrieves a list of all user addresses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching users")
    })
    public AddressResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                       @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                               defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userAddressServiceProcessor.findAllAsList("BACKOFFICE", locale);
    }

    @Auditable(action = "FIND_ALL_USER_ADDRESSES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find user addresses by multiple filters",
            description = "Retrieves a list of user addresses that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User address found successfully"),
            @ApiResponse(responseCode = "404", description = "Users not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public AddressResponse findByMultipleFilters(@Valid @RequestBody AddressMultipleFiltersRequest
                                                             addressMultipleFiltersRequest,
                                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale)
    {
        return userAddressServiceProcessor.findByMultipleFilters(addressMultipleFiltersRequest, "BACKOFFICE", locale);
    }

    @Auditable(action = "EXPORT_USER_ADDRESSES")
    @PostMapping("/export")
    @Operation(summary = "Export user addresses", 
            description = "Exports user addresses based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User addresses exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportUserAddresses(@RequestBody AddressMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export user addresses in {} format with filters: {}", format, filters);

            switch (format.toLowerCase()) {
                case "csv":
                    data = userAddressServiceProcessor.exportToCsv(filters, "BACKOFFICE", locale);
                    contentType = "text/csv";
                    filename = "user-addresses.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = userAddressServiceProcessor.exportToExcel(filters, "BACKOFFICE", locale);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "user-addresses.xlsx";
                    break;

                case "pdf":
                    data = userAddressServiceProcessor.exportToPdf(filters, "BACKOFFICE", locale);
                    contentType = "application/pdf";
                    filename = "user-addresses.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported user addresses in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export user addresses: " + e.getMessage();
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
