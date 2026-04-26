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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.CountryServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.CountryDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CountryMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CountryResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-locations/v1/system/country")
@Tag(name = "Country System Resource", description = "Operations related to managing countries (system)")
@RequiredArgsConstructor
public class CountrySystemResource {

    private final CountryServiceProcessor countryServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(CountrySystemResource.class);

    @Auditable(action = "CREATE_COUNTRY")
    @PostMapping("/create")
    @Operation(summary = "Create a new country", description = "Creates a new country and returns the created country details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Country created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<CountryResponse> create(@Valid @RequestBody final CreateCountryRequest createCountryRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(countryServiceProcessor.create(createCountryRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "UPDATE_COUNTRY")
    @PutMapping("/update")
    @Operation(summary = "Update country details", description = "Updates an existing country's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Country updated successfully"),
            @ApiResponse(responseCode = "404", description = "Country not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<CountryResponse> update(@Valid @RequestBody final EditCountryRequest editCountryRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return ResponseEntity.ok(countryServiceProcessor.update(editCountryRequest, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_COUNTRY_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find country by ID", description = "Retrieves a country by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Country found successfully"),
            @ApiResponse(responseCode = "404", description = "Country not found"),
            @ApiResponse(responseCode = "400", description = "Country id supplied invalid")
    })
    public ResponseEntity<CountryResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(countryServiceProcessor.findById(id, locale, "SYSTEM"));
    }

    @Auditable(action = "DELETE_COUNTRY")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a country by ID")
    public ResponseEntity<CountryResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(countryServiceProcessor.delete(id, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_COUNTRIES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all countries", description = "Retrieves a list of all countries")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Countries retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Country(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching countries")
    })
    public ResponseEntity<CountryResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(countryServiceProcessor.findAll(locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_COUNTRIES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find countries by multiple filters",
            description = "Retrieves a list of countries that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Country(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Country(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<CountryResponse> findByMultipleFilters(@Valid @RequestBody CountryMultipleFiltersRequest countryMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return ResponseEntity.ok(countryServiceProcessor.findByMultipleFilters(countryMultipleFiltersRequest, "SYSTEM", locale));
    }

    @Auditable(action = "EXPORT_COUNTRIES")
    @PostMapping("/export")
    @Operation(summary = "Export countries", 
            description = "Exports countries based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Countries exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportCountries(@RequestBody CountryMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export countries in {} format with filters: {}", format, filters);

            // First get the countries based on filters
            CountryResponse response = countryServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<CountryDto> countryList = response.getCountryDtoList();

            switch (format.toLowerCase()) {
                case "csv":
                    data = countryServiceProcessor.exportToCsv(countryList);
                    contentType = "text/csv";
                    filename = "countries.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = countryServiceProcessor.exportToExcel(countryList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "countries.xlsx";
                    break;

                case "pdf":
                    data = countryServiceProcessor.exportToPdf(countryList);
                    contentType = "application/pdf";
                    filename = "countries.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported countries in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export countries: " + e.getMessage();
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

    @Auditable(action = "IMPORT_COUNTRIES_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import countries from CSV", 
            description = "Imports countries from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Countries imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importCountriesFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import countries from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import countries: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = countryServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import countries from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import countries: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}