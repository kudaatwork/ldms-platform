package projectlx.co.zw.locationsmanagementservice.service.rest.system;

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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.DistrictServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.DistrictDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.DistrictMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.DistrictResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-locations/v1/system/district")
@Tag(name = "District System Resource", description = "Operations related to managing districts (system)")
@RequiredArgsConstructor
public class DistrictSystemResource {

    private final DistrictServiceProcessor districtServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(DistrictSystemResource.class);

    @Auditable(action = "CREATE_DISTRICT")
    @PostMapping("/create")
    @Operation(summary = "Create a new district", description = "Creates a new district and returns the created district details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "District created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<DistrictResponse> create(@Valid @RequestBody final CreateDistrictRequest createDistrictRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(districtServiceProcessor.create(createDistrictRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "UPDATE_DISTRICT")
    @PutMapping("/update")
    @Operation(summary = "Update district details", description = "Updates an existing district's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "District updated successfully"),
            @ApiResponse(responseCode = "404", description = "District not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<DistrictResponse> update(@Valid @RequestBody final EditDistrictRequest editDistrictRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return ResponseEntity.ok(districtServiceProcessor.update(editDistrictRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_DISTRICT_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find district by ID", description = "Retrieves a district by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "District found successfully"),
            @ApiResponse(responseCode = "404", description = "District not found"),
            @ApiResponse(responseCode = "400", description = "District id supplied invalid")
    })
    public ResponseEntity<DistrictResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(districtServiceProcessor.findById(id, locale, "SYSTEM"));
    }

    @Auditable(action = "DELETE_DISTRICT")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a district by ID")
    public ResponseEntity<DistrictResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(districtServiceProcessor.delete(id, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_DISTRICTS_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all districts", description = "Retrieves a list of all districts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Districts retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "District(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching districts")
    })
    public ResponseEntity<DistrictResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(districtServiceProcessor.findAll(locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_DISTRICTS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find districts by multiple filters",
            description = "Retrieves a list of districts that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "District(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "District(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<DistrictResponse> findByMultipleFilters(@Valid @RequestBody DistrictMultipleFiltersRequest districtMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(districtServiceProcessor.findByMultipleFilters(districtMultipleFiltersRequest, "SYSTEM", locale));
    }

    @Auditable(action = "EXPORT_DISTRICTS")
    @PostMapping("/export")
    @Operation(summary = "Export districts", 
            description = "Exports districts based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Districts exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportDistricts(@RequestBody DistrictMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export districts in {} format with filters: {}", format, filters);

            // First get the districts based on filters
            DistrictResponse response = districtServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<DistrictDto> districtList = response.getDistrictDtoList();

            switch (format.toLowerCase()) {
                case "csv":
                    data = districtServiceProcessor.exportToCsv(districtList);
                    contentType = "text/csv";
                    filename = "districts.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = districtServiceProcessor.exportToExcel(districtList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "districts.xlsx";
                    break;

                case "pdf":
                    data = districtServiceProcessor.exportToPdf(districtList);
                    contentType = "application/pdf";
                    filename = "districts.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported districts in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export districts: " + e.getMessage();
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

    @Auditable(action = "IMPORT_DISTRICTS_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import districts from CSV", 
            description = "Imports districts from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Districts imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importDistrictsFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import districts from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import districts: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = districtServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import districts from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import districts: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}