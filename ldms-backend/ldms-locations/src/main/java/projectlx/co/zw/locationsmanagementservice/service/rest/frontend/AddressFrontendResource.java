package projectlx.co.zw.locationsmanagementservice.service.rest.frontend;

import com.lowagie.text.DocumentException;
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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.AddressServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AddressDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AddressMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AddressResponse;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/frontend/address")
@Tag(name = "Address Frontend Resource", description = "Operations related to managing addresses")
@RequiredArgsConstructor
public class AddressFrontendResource {

    private final AddressServiceProcessor addressServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(AddressFrontendResource.class);

    @Auditable(action = "CREATE_ADDRESS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "CREATE_ADDRESS.toString())")
    @PostMapping("/create")
    @Operation(summary = "Create a new address", description = "Creates a new address and returns the created address details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody final CreateAddressRequest createAddressRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(addressServiceProcessor.create(createAddressRequest, locale, username));
    }

    @Auditable(action = "UPDATE_ADDRESS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "UPDATE_ADDRESS.toString())")
    @PutMapping("/update")
    @Operation(summary = "Update address details", description = "Updates an existing address's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address updated successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<AddressResponse> update(@Valid @RequestBody final EditAddressRequest editAddressRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(addressServiceProcessor.update(editAddressRequest, locale, username));
    }

    @Auditable(action = "FIND_ADDRESS_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "VIEW_ADDRESS_BY_ID.toString())")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find address by ID", description = "Retrieves an address by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address found successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "400", description = "Address id supplied invalid")
    })
    public ResponseEntity<?> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(addressServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_ADDRESS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "DELETE_ADDRESS.toString())")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete an address by ID")
    public ResponseEntity<?> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(addressServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_ADDRESSES_BY_LIST")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "VIEW_ALL_ADDRESSES_AS_A_LIST.toString())")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all addresses", description = "Retrieves a list of all addresses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Address(es) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching addresses")
    })
    public ResponseEntity<AddressResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(addressServiceProcessor.findAllAsAList(locale, username));
    }

    @Auditable(action = "FIND_ALL_ADDRESSES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "VIEW_ALL_ADDRESSES_BY_MULTIPLE_FILTERS.toString())")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find addresses by multiple filters",
            description = "Retrieves a list of addresses that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address(es) found successfully"),
            @ApiResponse(responseCode = "404", description = "Address(es) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<AddressResponse> findByMultipleFilters(@Valid @RequestBody AddressMultipleFiltersRequest addressMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(addressServiceProcessor.findByMultipleFilters(addressMultipleFiltersRequest, username, locale));
    }

    @Auditable(action = "EXPORT_ADDRESSES")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "EXPORT_ADDRESSES.toString())")
    @PostMapping("/export")
    @Operation(summary = "Export addresses", 
            description = "Exports addresses based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportAddresses(@RequestBody AddressMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export addresses in {} format with filters: {}", format, filters);

            // First get the addresses based on filters
            AddressResponse response = addressServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<AddressDto> addressList = response.getAddressDtoList();

            switch (format.toLowerCase()) {
                case "csv":
                    data = addressServiceProcessor.exportToCsv(addressList);
                    contentType = "text/csv";
                    filename = "addresses.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = addressServiceProcessor.exportToExcel(addressList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "addresses.xlsx";
                    break;

                case "pdf":
                    data = addressServiceProcessor.exportToPdf(addressList);
                    contentType = "application/pdf";
                    filename = "addresses.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported addresses in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export addresses: " + e.getMessage();
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

    @Auditable(action = "IMPORT_ADDRESSES_FROM_CSV")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.AddressRoles)." +
            "IMPORT_ADDRESSES.toString())")
    @PostMapping("/import-csv")
    @Operation(summary = "Import addresses from CSV", 
            description = "Imports addresses from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<?> importAddressesFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            logger.info("Incoming request to import addresses from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    "Failed to import addresses: Empty file"
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = addressServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import addresses from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                "Failed to import addresses: " + e.getMessage()
            );
        }
    }
}
