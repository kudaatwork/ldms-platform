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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.SuburbServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.SuburbDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.SuburbMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.SuburbResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/ldms-locations/v1/system/suburb")
@Tag(name = "Suburb System Resource", description = "Operations related to managing suburbs (system)")
@RequiredArgsConstructor
public class SuburbSystemResource {

    private final SuburbServiceProcessor suburbServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(SuburbSystemResource.class);

    @Auditable(action = "CREATE_SUBURB")
    @PostMapping("/create")
    @Operation(summary = "Create a new suburb", description = "Creates a new suburb and returns the created suburb details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Suburb created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<SuburbResponse> create(@Valid @RequestBody final CreateSuburbRequest createSuburbRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(suburbServiceProcessor.create(createSuburbRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "UPDATE_SUBURB")
    @PutMapping("/update")
    @Operation(summary = "Update suburb details", description = "Updates an existing suburb's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Suburb updated successfully"),
            @ApiResponse(responseCode = "404", description = "Suburb not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<SuburbResponse> update(@Valid @RequestBody final EditSuburbRequest editSuburbRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return ResponseEntity.ok(suburbServiceProcessor.update(editSuburbRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_SUBURB_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find suburb by ID", description = "Retrieves a suburb by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suburb found successfully"),
            @ApiResponse(responseCode = "404", description = "Suburb not found"),
            @ApiResponse(responseCode = "400", description = "Suburb id supplied invalid")
    })
    public ResponseEntity<SuburbResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(suburbServiceProcessor.findById(id, locale, "SYSTEM"));
    }

    @Auditable(action = "DELETE_SUBURB")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a suburb by ID")
    public ResponseEntity<SuburbResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(suburbServiceProcessor.delete(id, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_SUBURBS_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all suburbs", description = "Retrieves a list of all suburbs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suburbs retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Suburb(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching suburbs")
    })
    public ResponseEntity<SuburbResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(suburbServiceProcessor.findAll(locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_SUBURBS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find suburbs by multiple filters",
            description = "Retrieves a list of suburbs that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suburb(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Suburb(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<SuburbResponse> findByMultipleFilters(@Valid @RequestBody SuburbMultipleFiltersRequest suburbMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(suburbServiceProcessor.findByMultipleFilters(suburbMultipleFiltersRequest, "SYSTEM", locale));
    }

    @Auditable(action = "EXPORT_SUBURBS")
    @PostMapping("/export")
    @Operation(summary = "Export suburbs", 
            description = "Exports suburbs based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suburbs exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportSuburbs(@RequestBody SuburbMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export suburbs in {} format with filters: {}", format, filters);

            filters.setPage(0);
            filters.setSize(Integer.MAX_VALUE);
            SuburbResponse response = suburbServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<SuburbDto> suburbList = response.getSuburbDtoPage() != null
                    ? response.getSuburbDtoPage().getContent()
                    : new ArrayList<>();

            switch (format.toLowerCase()) {
                case "csv":
                    data = suburbServiceProcessor.exportToCsv(suburbList);
                    contentType = "text/csv";
                    filename = "suburbs.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = suburbServiceProcessor.exportToExcel(suburbList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "suburbs.xlsx";
                    break;

                case "pdf":
                    data = suburbServiceProcessor.exportToPdf(suburbList);
                    contentType = "application/pdf";
                    filename = "suburbs.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported suburbs in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export suburbs: " + e.getMessage();
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

    @Auditable(action = "IMPORT_SUBURBS_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import suburbs from CSV", 
            description = "Imports suburbs from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suburbs imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importSuburbsFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import suburbs from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import suburbs: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = suburbServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import suburbs from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import suburbs: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}