package projectlx.co.zw.locationsmanagementservice.service.rest.backoffice;

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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.AdministrativeLevelServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.AdministrativeLevelDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAdministrativeLevelRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.AdministrativeLevelMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AdministrativeLevelResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/ldms-locations/v1/backoffice/administrative-level")
@Tag(name = "Administrative Level Backoffice Resource", description = "Operations related to managing administrative levels (backoffice)")
@RequiredArgsConstructor
public class AdministrativeLevelBackofficeResource {

    private final AdministrativeLevelServiceProcessor administrativeLevelServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(AdministrativeLevelBackofficeResource.class);

    @Auditable(action = "CREATE_ADMINISTRATIVE_LEVEL")
    @PostMapping("/create")
    @Operation(summary = "Create a new administrative level", description = "Creates a new administrative level and returns the created administrative level details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Administrative level created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<AdministrativeLevelResponse> create(@Valid @RequestBody final CreateAdministrativeLevelRequest createAdministrativeLevelRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(administrativeLevelServiceProcessor.create(createAdministrativeLevelRequest, locale, "BACKOFFICE"));
    }

    @Auditable(action = "UPDATE_ADMINISTRATIVE_LEVEL")
    @PutMapping("/update")
    @Operation(summary = "Update administrative level details", description = "Updates an existing administrative level's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Administrative level updated successfully"),
            @ApiResponse(responseCode = "404", description = "Administrative level not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<AdministrativeLevelResponse> update(@Valid @RequestBody final EditAdministrativeLevelRequest editAdministrativeLevelRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return ResponseEntity.ok(administrativeLevelServiceProcessor.update(editAdministrativeLevelRequest, locale, "BACKOFFICE"));
    }

    @Auditable(action = "FIND_ADMINISTRATIVE_LEVEL_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find administrative level by ID", description = "Retrieves an administrative level by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administrative level found successfully"),
            @ApiResponse(responseCode = "404", description = "Administrative level not found"),
            @ApiResponse(responseCode = "400", description = "Administrative level id supplied invalid")
    })
    public ResponseEntity<AdministrativeLevelResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(administrativeLevelServiceProcessor.findById(id, locale, "BACKOFFICE"));
    }

    @Auditable(action = "DELETE_ADMINISTRATIVE_LEVEL")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete an administrative level by ID")
    public ResponseEntity<AdministrativeLevelResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(administrativeLevelServiceProcessor.delete(id, locale, "BACKOFFICE"));
    }

    @Auditable(action = "FIND_ALL_ADMINISTRATIVE_LEVELS_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all administrative levels", description = "Retrieves a list of all administrative levels")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administrative levels retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Administrative level(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching administrative levels")
    })
    public ResponseEntity<AdministrativeLevelResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(administrativeLevelServiceProcessor.findAll(locale, "BACKOFFICE"));
    }

    @Auditable(action = "FIND_ALL_ADMINISTRATIVE_LEVELS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find administrative levels by multiple filters",
            description = "Retrieves a list of administrative levels that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administrative level(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Administrative level(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<AdministrativeLevelResponse> findByMultipleFilters(@Valid @RequestBody AdministrativeLevelMultipleFiltersRequest administrativeLevelMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(administrativeLevelServiceProcessor.findByMultipleFilters(administrativeLevelMultipleFiltersRequest, "BACKOFFICE", locale));
    }

    @Auditable(action = "EXPORT_ADMINISTRATIVE_LEVELS")
    @PostMapping("/export")
    @Operation(summary = "Export administrative levels", 
            description = "Exports administrative levels based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administrative levels exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportAdministrativeLevels(@RequestBody AdministrativeLevelMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export administrative levels in {} format with filters: {}", format, filters);

            filters.setPage(0);
            filters.setSize(Integer.MAX_VALUE);
            AdministrativeLevelResponse response = administrativeLevelServiceProcessor.findByMultipleFilters(filters, "BACKOFFICE", locale);
            List<AdministrativeLevelDto> administrativeLevelList = response.getAdministrativeLevelDtoPage() != null
                    ? response.getAdministrativeLevelDtoPage().getContent()
                    : new ArrayList<>();

            switch (format.toLowerCase()) {
                case "csv":
                    data = administrativeLevelServiceProcessor.exportToCsv(administrativeLevelList);
                    contentType = "text/csv";
                    filename = "administrative-levels.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = administrativeLevelServiceProcessor.exportToExcel(administrativeLevelList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "administrative-levels.xlsx";
                    break;

                case "pdf":
                    data = administrativeLevelServiceProcessor.exportToPdf(administrativeLevelList);
                    contentType = "application/pdf";
                    filename = "administrative-levels.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported administrative levels in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export administrative levels: " + e.getMessage();
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

    @Auditable(action = "IMPORT_ADMINISTRATIVE_LEVELS_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import administrative levels from CSV", 
            description = "Imports administrative levels from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administrative levels imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importAdministrativeLevelsFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import administrative levels from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import administrative levels: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = administrativeLevelServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import administrative levels from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import administrative levels: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}