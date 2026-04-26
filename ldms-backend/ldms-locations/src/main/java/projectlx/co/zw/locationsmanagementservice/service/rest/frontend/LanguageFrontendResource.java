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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LanguageServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LanguageDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LanguageMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LanguageResponse;
import projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-locations/v1/frontend/language")
@Tag(name = "Language Frontend Resource", description = "Operations related to managing languages")
@RequiredArgsConstructor
public class LanguageFrontendResource {

    private final LanguageServiceProcessor languageServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(LanguageFrontendResource.class);

    @Auditable(action = "CREATE_LANGUAGE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "CREATE_LANGUAGE.toString())")
    @PostMapping("/create")
    @Operation(summary = "Create a new language", description = "Creates a new language and returns the created language details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Language created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public LanguageResponse create(@Valid @RequestBody final CreateLanguageRequest createLanguageRequest,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return languageServiceProcessor.create(createLanguageRequest, locale, username);
    }

    @Auditable(action = "UPDATE_LANGUAGE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "UPDATE_LANGUAGE.toString())")
    @PutMapping("/update")
    @Operation(summary = "Update language details", description = "Updates an existing language's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Language updated successfully"),
            @ApiResponse(responseCode = "404", description = "Language not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public LanguageResponse update(@Valid @RequestBody final EditLanguageRequest editLanguageRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return languageServiceProcessor.update(editLanguageRequest, locale, username);
    }

    @Auditable(action = "FIND_LANGUAGE_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "VIEW_LANGUAGE_BY_ID.toString())")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find language by ID", description = "Retrieves a language by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Language found successfully"),
            @ApiResponse(responseCode = "404", description = "Language not found"),
            @ApiResponse(responseCode = "400", description = "Language id supplied invalid")
    })
    public LanguageResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return languageServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_LANGUAGE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "DELETE_LANGUAGE.toString())")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a language by ID")
    public LanguageResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return languageServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_LANGUAGES_BY_LIST")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "VIEW_ALL_LANGUAGES_AS_A_LIST.toString())")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all languages", description = "Retrieves a list of all languages")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Languages retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Language(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching languages")
    })
    public LanguageResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return languageServiceProcessor.findAll(locale, username);
    }

    @Auditable(action = "FIND_ALL_LANGUAGES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "VIEW_ALL_LANGUAGES_BY_MULTIPLE_FILTERS.toString())")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find languages by multiple filters",
            description = "Retrieves a list of languages that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Language(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Language(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public LanguageResponse findByMultipleFilters(@Valid @RequestBody LanguageMultipleFiltersRequest languageMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return languageServiceProcessor.findByMultipleFilters(languageMultipleFiltersRequest, username, locale);
    }

    @Auditable(action = "EXPORT_LANGUAGES")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "EXPORT_LANGUAGES.toString())")
    @PostMapping("/export")
    @Operation(summary = "Export languages", 
            description = "Exports languages based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Languages exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportLanguages(@RequestBody LanguageMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export languages in {} format with filters: {}", format, filters);
            
            // Get the data based on filters
            LanguageResponse response = languageServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<LanguageDto> dtoList = response.getLanguageDtoList();

            switch (format.toLowerCase()) {
                case "csv":
                    data = languageServiceProcessor.exportToCsv(dtoList);
                    contentType = "text/csv";
                    filename = "languages.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = languageServiceProcessor.exportToExcel(dtoList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "languages.xlsx";
                    break;

                case "pdf":
                    data = languageServiceProcessor.exportToPdf(dtoList);
                    contentType = "application/pdf";
                    filename = "languages.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported languages in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export languages: " + e.getMessage();
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

    @Auditable(action = "IMPORT_LANGUAGES")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.LanguageRoles)." +
            "IMPORT_LANGUAGES.toString())")
    @PostMapping("/import-csv")
    @Operation(summary = "Import languages from CSV", 
            description = "Imports languages from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Languages imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importLanguagesFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            logger.info("Incoming request to import languages from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import languages: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = languageServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import languages from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import languages: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}