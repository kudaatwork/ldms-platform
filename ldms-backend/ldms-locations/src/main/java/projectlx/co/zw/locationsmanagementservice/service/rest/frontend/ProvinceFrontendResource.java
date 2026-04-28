package projectlx.co.zw.locationsmanagementservice.service.rest.frontend;

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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.ProvinceServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.ProvinceMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.ProvinceResponse;
import projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/ldms-locations/v1/frontend/province")
@Tag(name = "Province Frontend Resource", description = "Operations related to managing provinces")
@RequiredArgsConstructor
public class ProvinceFrontendResource {

    private final ProvinceServiceProcessor provinceServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(ProvinceFrontendResource.class);

    @Auditable(action = "CREATE_PROVINCE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "CREATE_PROVINCE.toString())")
    @PostMapping("/create")
    @Operation(summary = "Create a new province", description = "Creates a new province and returns the created province details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Province created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<ProvinceResponse> create(@Valid @RequestBody final CreateProvinceRequest createProvinceRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(provinceServiceProcessor.create(createProvinceRequest, locale, username));
    }

    @Auditable(action = "UPDATE_PROVINCE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "UPDATE_PROVINCE.toString())")
    @PutMapping("/update")
    @Operation(summary = "Update province details", description = "Updates an existing province's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Province updated successfully"),
            @ApiResponse(responseCode = "404", description = "Province not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ProvinceResponse> update(@Valid @RequestBody final EditProvinceRequest editProvinceRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(provinceServiceProcessor.update(editProvinceRequest, locale, username));
    }

    @Auditable(action = "FIND_PROVINCE_BY_ID")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "VIEW_PROVINCE_BY_ID.toString())")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find province by ID", description = "Retrieves a province by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Province found successfully"),
            @ApiResponse(responseCode = "404", description = "Province not found"),
            @ApiResponse(responseCode = "400", description = "Province id supplied invalid")
    })
    public ResponseEntity<ProvinceResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(provinceServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_PROVINCE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "DELETE_PROVINCE.toString())")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a province by ID")
    public ResponseEntity<ProvinceResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(provinceServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_PROVINCES_BY_LIST")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "VIEW_ALL_PROVINCES_AS_A_LIST.toString())")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all provinces", description = "Retrieves a list of all provinces")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provinces retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Province(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching provinces")
    })
    public ResponseEntity<ProvinceResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(provinceServiceProcessor.findAll(locale, username));
    }

    @Auditable(action = "FIND_ALL_PROVINCES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "VIEW_ALL_PROVINCES_BY_MULTIPLE_FILTERS.toString())")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find provinces by multiple filters",
            description = "Retrieves a list of provinces that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Province(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Province(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<ProvinceResponse> findByMultipleFilters(@Valid @RequestBody ProvinceMultipleFiltersRequest provinceMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(provinceServiceProcessor.findByMultipleFilters(provinceMultipleFiltersRequest, username, locale));
    }

    @Auditable(action = "EXPORT_PROVINCES")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "EXPORT_PROVINCES.toString())")
    @PostMapping("/export")
    @Operation(summary = "Export provinces", 
            description = "Exports provinces based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provinces exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportProvinces(@RequestBody ProvinceMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export provinces in {} format with filters: {}", format, filters);

            switch (format.toLowerCase()) {
                case "csv":
                    data = provinceServiceProcessor.exportToCsv(filters, locale, username);
                    contentType = "text/csv";
                    filename = "provinces.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = provinceServiceProcessor.exportToExcel(filters, locale, username);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "provinces.xlsx";
                    break;

                case "pdf":
                    data = provinceServiceProcessor.exportToPdf(filters, locale, username);
                    contentType = "application/pdf";
                    filename = "provinces.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported provinces in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export provinces: " + e.getMessage();
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

    @Auditable(action = "IMPORT_PROVINCES_FROM_CSV")
    @PreAuthorize("hasRole(T(projectlx.co.zw.locationsmanagementservice.utils.security.ProvinceRoles)." +
            "IMPORT_PROVINCES.toString())")
    @PostMapping("/import-csv")
    @Operation(summary = "Import provinces from CSV", 
            description = "Imports provinces from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Provinces imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importProvincesFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            logger.info("Incoming request to import provinces from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import provinces: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = provinceServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.status(summary.getStatusCode()).body(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import provinces from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import provinces: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}