package projectlx.inventory.management.service.rest.system;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.service.processor.api.WarehouseLocationServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.WarehouseLocationDto;
import projectlx.inventory.management.utils.requests.CreateWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.EditWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.WarehouseLocationMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.WarehouseLocationResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/warehouse-locations")
@Tag(name = "Warehouse Location System Resource", description = "Operations related to managing warehouse locations (system)")
@RequiredArgsConstructor
public class WarehouseLocationSystemResource {

    private final WarehouseLocationServiceProcessor warehouseLocationServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(WarehouseLocationSystemResource.class);

    @Auditable(action = "CREATE_WAREHOUSE_LOCATION")
    @PostMapping({"", "/"})
    @Operation(summary = "Create a new warehouse location")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Warehouse location created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<WarehouseLocationResponse> create(@Valid @RequestBody final CreateWarehouseLocationRequest request,
                                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                            final Locale locale) {
        return ResponseEntity.ok(warehouseLocationServiceProcessor.create(request, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_WAREHOUSE_LOCATION_BY_ID")
    @GetMapping("/{id}")
    @Operation(summary = "Find warehouse location by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warehouse location found successfully"),
            @ApiResponse(responseCode = "404", description = "Warehouse location not found"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied")
    })
    public ResponseEntity<WarehouseLocationResponse> findById(@PathVariable("id") final Long id,
                                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                              final Locale locale) {
        return ResponseEntity.ok(warehouseLocationServiceProcessor.findById(id, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_ALL_WAREHOUSE_LOCATIONS_BY_LIST")
    @GetMapping("/list")
    @Operation(summary = "Get all warehouse locations as a list")
    public ResponseEntity<WarehouseLocationResponse> listAll(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                             @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                             final Locale locale) {
        return ResponseEntity.ok(warehouseLocationServiceProcessor.findAllAsList(locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_WAREHOUSE_LOCATIONS_BY_MULTIPLE_FILTERS")
    @PostMapping("/search")
    @Operation(summary = "Find warehouse locations by multiple filters")
    public ResponseEntity<WarehouseLocationResponse> search(@Valid @RequestBody final WarehouseLocationMultipleFiltersRequest request,
                                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                            final Locale locale) {
        return ResponseEntity.ok(warehouseLocationServiceProcessor.findByMultipleFilters(request, "SYSTEM", locale));
    }

    @Auditable(action = "UPDATE_WAREHOUSE_LOCATION")
    @PutMapping("/{id}")
    @Operation(summary = "Update a warehouse location")
    public ResponseEntity<WarehouseLocationResponse> update(@PathVariable("id") final Long id,
                                                            @Valid @RequestBody final EditWarehouseLocationRequest request,
                                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                            final Locale locale) {
        request.setWarehouseLocationId(id);
        return ResponseEntity.ok(warehouseLocationServiceProcessor.update(request, "SYSTEM", locale));
    }

    @Auditable(action = "DELETE_WAREHOUSE_LOCATION")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a warehouse location")
    public ResponseEntity<WarehouseLocationResponse> delete(@PathVariable("id") final Long id,
                                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                            final Locale locale) {
        return ResponseEntity.ok(warehouseLocationServiceProcessor.delete(id, locale, "SYSTEM"));
    }

    @Auditable(action = "IMPORT_WAREHOUSE_LOCATIONS_FROM_CSV")
    @PostMapping("/import/csv")
    @Operation(summary = "Import warehouse locations from CSV")
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import warehouse locations from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import warehouse locations: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = warehouseLocationServiceProcessor.importWarehouseLocationFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import warehouse locations from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import warehouse locations: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }

    @Auditable(action = "EXPORT_WAREHOUSE_LOCATIONS_TO_CSV")
    @PostMapping("/export/csv")
    @Operation(summary = "Export provided warehouse locations to CSV")
    public ResponseEntity<byte[]> exportCsv(@RequestBody final List<WarehouseLocationDto> items) {
        try {
            byte[] data = warehouseLocationServiceProcessor.exportToCsv(items);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=warehouse_locations.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(data);
        } catch (Exception e) {
            String errorMsg = "Failed to export warehouse locations to CSV: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "EXPORT_WAREHOUSE_LOCATIONS_TO_EXCEL")
    @PostMapping("/export/excel")
    @Operation(summary = "Export provided warehouse locations to Excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody final List<WarehouseLocationDto> items) {
        try {
            byte[] data = warehouseLocationServiceProcessor.exportToExcel(items);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=warehouse_locations.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (IOException e) {
            String errorMsg = "Failed to export warehouse locations to Excel: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Auditable(action = "EXPORT_WAREHOUSE_LOCATIONS_TO_PDF")
    @PostMapping("/export/pdf")
    @Operation(summary = "Export provided warehouse locations to PDF")
    public ResponseEntity<byte[]> exportPdf(@RequestBody final List<WarehouseLocationDto> items) {
        try {
            byte[] data = warehouseLocationServiceProcessor.exportToPdf(items);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=warehouse_locations.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (DocumentException e) {
            String errorMsg = "Failed to export warehouse locations to PDF: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }
    }
}
