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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.CityServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CityDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CityMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CityResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/ldms-locations/v1/system/city")
@Tag(name = "City System Resource", description = "Operations related to managing cities (system)")
@RequiredArgsConstructor
public class CitySystemResource {

    private final CityServiceProcessor cityServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(CitySystemResource.class);

    @Auditable(action = "CREATE_CITY")
    @PostMapping("/create")
    @Operation(summary = "Create a new city", description = "Creates a new city and returns the created city details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "City created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<CityResponse> create(@Valid @RequestBody final CreateCityRequest createCityRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(cityServiceProcessor.create(createCityRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "UPDATE_CITY")
    @PutMapping("/update")
    @Operation(summary = "Update city details", description = "Updates an existing city's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "City updated successfully"),
            @ApiResponse(responseCode = "404", description = "City not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<CityResponse> update(@Valid @RequestBody final EditCityRequest editCityRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return ResponseEntity.ok(cityServiceProcessor.update(editCityRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_CITY_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find city by ID", description = "Retrieves a city by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "City found successfully"),
            @ApiResponse(responseCode = "404", description = "City not found"),
            @ApiResponse(responseCode = "400", description = "City id supplied invalid")
    })
    public ResponseEntity<CityResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(cityServiceProcessor.findById(id, locale, "SYSTEM"));
    }

    @Auditable(action = "DELETE_CITY")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a city by ID")
    public ResponseEntity<CityResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(cityServiceProcessor.delete(id, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_CITIES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all cities", description = "Retrieves a list of all cities")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cities retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "City(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching cities")
    })
    public ResponseEntity<CityResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(cityServiceProcessor.findAll(locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_CITIES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find cities by multiple filters",
            description = "Retrieves a list of cities that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "City(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "City(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<CityResponse> findByMultipleFilters(@Valid @RequestBody CityMultipleFiltersRequest cityMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(cityServiceProcessor.findByMultipleFilters(cityMultipleFiltersRequest, "SYSTEM", locale));
    }

    @Auditable(action = "EXPORT_CITIES")
    @PostMapping("/export")
    @Operation(summary = "Export cities",
            description = "Exports cities based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cities exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportCities(@RequestBody CityMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export cities in {} format with filters: {}", format, filters);

            filters.setPage(0);
            filters.setSize(Integer.MAX_VALUE);
            CityResponse response = cityServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<CityDto> cityList = response.getCityDtoPage() != null
                    ? response.getCityDtoPage().getContent()
                    : new ArrayList<>();

            switch (format.toLowerCase()) {
                case "csv":
                    data = cityServiceProcessor.exportToCsv(cityList);
                    contentType = "text/csv";
                    filename = "cities.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = cityServiceProcessor.exportToExcel(cityList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "cities.xlsx";
                    break;

                case "pdf":
                    data = cityServiceProcessor.exportToPdf(cityList);
                    contentType = "application/pdf";
                    filename = "cities.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported cities in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export cities: " + e.getMessage();
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

    @Auditable(action = "IMPORT_CITIES_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import cities from CSV",
            description = "Imports cities from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cities imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importCitiesFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import cities from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import cities: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = cityServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import cities from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import cities: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}