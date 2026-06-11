package projectlx.inventory.management.service.rest.frontend;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.service.processor.api.StockAdjustmentServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.StockAdjustmentDto;
import projectlx.inventory.management.utils.requests.CreateStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.EditStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.StockAdjustmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.StockAdjustmentResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/stock-adjustment")
@Tag(name = "Stock Adjustment Frontend Resource", description = "Operations related to managing stock adjustments")
@RequiredArgsConstructor
public class StockAdjustmentFrontendResource {

    private final StockAdjustmentServiceProcessor stockAdjustmentServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(StockAdjustmentFrontendResource.class);

    @Auditable(action = "CREATE_STOCK_ADJUSTMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new stock adjustment", description = "Creates a new stock adjustment and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock adjustment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<StockAdjustmentResponse> create(@Valid @RequestBody final CreateStockAdjustmentRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(stockAdjustmentServiceProcessor.create(request, locale, username));
    }

    @Auditable(action = "UPDATE_STOCK_ADJUSTMENT")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update stock adjustment details", description = "Updates an existing stock adjustment's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock adjustment updated successfully"),
            @ApiResponse(responseCode = "404", description = "Stock adjustment not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<StockAdjustmentResponse> update(@Valid @RequestBody final EditStockAdjustmentRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(stockAdjustmentServiceProcessor.update(request, username, locale));
    }

    @Auditable(action = "FIND_STOCK_ADJUSTMENT_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find stock adjustment by ID", description = "Retrieves a stock adjustment by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock adjustment found successfully"),
            @ApiResponse(responseCode = "404", description = "Stock adjustment not found"),
            @ApiResponse(responseCode = "400", description = "Stock adjustment id supplied invalid")
    })
    public ResponseEntity<StockAdjustmentResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(stockAdjustmentServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_STOCK_ADJUSTMENT")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a stock adjustment by ID")
    public ResponseEntity<StockAdjustmentResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(stockAdjustmentServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_STOCK_ADJUSTMENTS_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all stock adjustments", description = "Retrieves a list of all stock adjustments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock adjustments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Stock adjustment(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching stock adjustments")
    })
    public ResponseEntity<StockAdjustmentResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(stockAdjustmentServiceProcessor.findAllAsList(locale, username));
    }

    @Auditable(action = "FIND_ALL_STOCK_ADJUSTMENTS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find stock adjustments by multiple filters",
            description = "Retrieves a list of stock adjustments that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock adjustment(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Stock adjustment(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<StockAdjustmentResponse> findByMultipleFilters(@Valid @RequestBody StockAdjustmentMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(stockAdjustmentServiceProcessor.findByMultipleFilters(filters, username, locale));
    }

    @Auditable(action = "EXPORT_STOCK_ADJUSTMENTS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export stock adjustments",
            description = "Exports stock adjustments based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock adjustments exported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid export format"),
        @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody StockAdjustmentMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export stock adjustments in {} format with filters: {}", format, filters);
            StockAdjustmentResponse response = stockAdjustmentServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<StockAdjustmentDto> list = InventoryExportSupport.resolveExportItems(
                    response.getStockAdjustmentDtoPage(), response.getStockAdjustmentDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = stockAdjustmentServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "stock_adjustments.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = stockAdjustmentServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "stock_adjustments.xlsx";
                    break;
                case "pdf":
                    data = stockAdjustmentServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "stock_adjustments.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export stock adjustments: " + e.getMessage();
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

    @Auditable(action = "IMPORT_STOCK_ADJUSTMENTS_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import stock adjustments from CSV",
            description = "Imports stock adjustments from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock adjustments imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            logger.info("Incoming request by '{}' to import stock adjustments from CSV file: {}", username, file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import stock adjustments: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = stockAdjustmentServiceProcessor.importStockAdjustmentFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import stock adjustments from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import stock adjustments: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
