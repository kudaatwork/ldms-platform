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
import projectlx.co.zw.locationsmanagementservice.service.processor.api.GeoCoordinatesServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.GeoCoordinatesDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.GeoCoordinatesMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.GeoCoordinatesResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/system/geo-coordinates")
@Tag(name = "Geo Coordinates System Resource", description = "Operations related to managing geo coordinates (system)")
@RequiredArgsConstructor
public class GeoCoordinatesSystemResource {

    private final GeoCoordinatesServiceProcessor geoCoordinatesServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(GeoCoordinatesSystemResource.class);

    @Auditable(action = "CREATE_GEO_COORDINATES")
    @PostMapping("/create")
    @Operation(summary = "Create new geo coordinates", description = "Creates new geo coordinates and returns the created geo coordinates details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Geo coordinates created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public GeoCoordinatesResponse create(@Valid @RequestBody final CreateGeoCoordinatesRequest createGeoCoordinatesRequest,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return geoCoordinatesServiceProcessor.create(createGeoCoordinatesRequest, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_GEO_COORDINATES")
    @PutMapping("/update")
    @Operation(summary = "Update geo coordinates details", description = "Updates existing geo coordinates details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Geo coordinates updated successfully"),
            @ApiResponse(responseCode = "404", description = "Geo coordinates not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public GeoCoordinatesResponse update(@Valid @RequestBody final EditGeoCoordinatesRequest editGeoCoordinatesRequest,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return geoCoordinatesServiceProcessor.update(editGeoCoordinatesRequest, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_GEO_COORDINATES_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find geo coordinates by ID", description = "Retrieves geo coordinates by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geo coordinates found successfully"),
            @ApiResponse(responseCode = "404", description = "Geo coordinates not found"),
            @ApiResponse(responseCode = "400", description = "Geo coordinates id supplied invalid")
    })
    public GeoCoordinatesResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return geoCoordinatesServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_GEO_COORDINATES")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete geo coordinates by ID")
    public GeoCoordinatesResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return geoCoordinatesServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_GEO_COORDINATES_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all geo coordinates", description = "Retrieves a list of all geo coordinates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geo coordinates retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Geo coordinates not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching geo coordinates")
    })
    public GeoCoordinatesResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return geoCoordinatesServiceProcessor.findAll(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_GEO_COORDINATES_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find geo coordinates by multiple filters",
            description = "Retrieves a list of geo coordinates that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geo coordinates found successfully"),
            @ApiResponse(responseCode = "404", description = "Geo coordinates not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public GeoCoordinatesResponse findByMultipleFilters(@Valid @RequestBody GeoCoordinatesMultipleFiltersRequest geoCoordinatesMultipleFiltersRequest,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return geoCoordinatesServiceProcessor.findByMultipleFilters(geoCoordinatesMultipleFiltersRequest, "SYSTEM", locale);
    }

    @Auditable(action = "EXPORT_GEO_COORDINATES")
    @PostMapping("/export")
    @Operation(summary = "Export geo coordinates", 
            description = "Exports geo coordinates based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geo coordinates exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportGeoCoordinates(@RequestBody GeoCoordinatesMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;

        try {
            logger.info("Incoming request to export geo coordinates in {} format with filters: {}", format, filters);
            
            // Get the data based on filters
            GeoCoordinatesResponse response = geoCoordinatesServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<GeoCoordinatesDto> dtoList = response.getGeoCoordinatesDtoList();

            switch (format.toLowerCase()) {
                case "csv":
                    data = geoCoordinatesServiceProcessor.exportToCsv(dtoList);
                    contentType = "text/csv";
                    filename = "geo-coordinates.csv";
                    break;

                case "excel":
                case "xlsx":
                    data = geoCoordinatesServiceProcessor.exportToExcel(dtoList);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "geo-coordinates.xlsx";
                    break;

                case "pdf":
                    data = geoCoordinatesServiceProcessor.exportToPdf(dtoList);
                    contentType = "application/pdf";
                    filename = "geo-coordinates.pdf";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            logger.info("Successfully exported geo coordinates in {} format. Data size: {} bytes", format, data.length);

        } catch (Exception e) {
            String errorMsg = "Failed to export geo coordinates: " + e.getMessage();
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

    @Auditable(action = "IMPORT_GEO_COORDINATES")
    @PostMapping("/import-csv")
    @Operation(summary = "Import geo coordinates from CSV", 
            description = "Imports geo coordinates from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geo coordinates imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importGeoCoordinatesFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import geo coordinates from CSV file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ImportSummary(400, false, "Failed to import geo coordinates: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = geoCoordinatesServiceProcessor.importFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }

        } catch (IOException e) {
            logger.error("Failed to import geo coordinates from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ImportSummary(500, false, "Failed to import geo coordinates: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}