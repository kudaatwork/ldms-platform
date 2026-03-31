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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocalizedNameServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocalizedNameDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocalizedNameMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocalizedNameResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/system/localized-name")
@Tag(name = "Localized Name System Resource", description = "Operations related to managing localized names (system)")
@RequiredArgsConstructor
public class LocalizedNameSystemResource {

    private final LocalizedNameServiceProcessor localizedNameServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(LocalizedNameSystemResource.class);

    @Auditable(action = "CREATE_LOCALIZED_NAME")
    @PostMapping("/create")
    @Operation(summary = "Create a new localized name", description = "Creates a new localized name and returns the created localized name details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Localized name created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public LocalizedNameResponse create(@Valid @RequestBody final CreateLocalizedNameRequest createLocalizedNameRequest,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return localizedNameServiceProcessor.create(createLocalizedNameRequest, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_LOCALIZED_NAME")
    @PutMapping("/update")
    @Operation(summary = "Update localized name details", description = "Updates an existing localized name's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Localized name updated successfully"),
            @ApiResponse(responseCode = "404", description = "Localized name not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public LocalizedNameResponse update(@Valid @RequestBody final EditLocalizedNameRequest editLocalizedNameRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return localizedNameServiceProcessor.update(editLocalizedNameRequest, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_LOCALIZED_NAME_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find localized name by ID", description = "Retrieves a localized name by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localized name found successfully"),
            @ApiResponse(responseCode = "404", description = "Localized name not found"),
            @ApiResponse(responseCode = "400", description = "Localized name id supplied invalid")
    })
    public LocalizedNameResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return localizedNameServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_LOCALIZED_NAME")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a localized name by ID")
    public LocalizedNameResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return localizedNameServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_LOCALIZED_NAMES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all localized names", description = "Retrieves a list of all localized names")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localized names retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Localized name(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching localized names")
    })
    public LocalizedNameResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return localizedNameServiceProcessor.findAll(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_LOCALIZED_NAMES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find localized names by multiple filters",
            description = "Retrieves a list of localized names that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localized name(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Localized name(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public LocalizedNameResponse findByMultipleFilters(@Valid @RequestBody LocalizedNameMultipleFiltersRequest localizedNameMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return localizedNameServiceProcessor.findByMultipleFilters(localizedNameMultipleFiltersRequest, "SYSTEM", locale);
    }

    @Auditable(action = "EXPORT_LOCALIZED_NAMES")
    @PostMapping("/export")
    @Operation(summary = "Export localized names", 
            description = "Exports localized names based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localized names exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportLocalizedNames(@RequestBody LocalizedNameMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export localized names in {} format with filters: {}", format, filters);
            
            // Get the data based on filters
            LocalizedNameResponse response = localizedNameServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<LocalizedNameDto> dtoList = response.getLocalizedNameDtoList();

            switch (format.toLowerCase()) {
                case "csv":
                    data = localizedNameServiceProcessor.exportToCsv(dtoList);
                    contentType = "text/csv";
                    filename = "localized-names.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = localizedNameServiceProcessor.exportToExcel(dtoList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "localized-names.xlsx";
                    break;

                case "pdf":
                    data = localizedNameServiceProcessor.exportToPdf(dtoList);
                    contentType = "application/pdf";
                    filename = "localized-names.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported localized names in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export localized names: " + e.getMessage();
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

    @Auditable(action = "IMPORT_LOCALIZED_NAMES")
    @PostMapping("/import-csv")
    @Operation(summary = "Import localized names from CSV", 
            description = "Imports localized names from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localized names imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importLocalizedNamesFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import localized names from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import localized names: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = localizedNameServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import localized names from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import localized names: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}