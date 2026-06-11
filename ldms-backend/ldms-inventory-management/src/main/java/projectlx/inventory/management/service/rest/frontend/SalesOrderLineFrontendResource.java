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
import projectlx.inventory.management.service.processor.api.SalesOrderLineServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.SalesOrderLineDto;
import projectlx.inventory.management.utils.requests.CreateSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.SalesOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesOrderLineResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/sales-order-line")
@Tag(name = "Sales Order Line Frontend Resource", description = "Operations related to managing sales order lines")
@RequiredArgsConstructor
public class SalesOrderLineFrontendResource {

    private final SalesOrderLineServiceProcessor salesOrderLineServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(SalesOrderLineFrontendResource.class);

    @Auditable(action = "CREATE_SALES_ORDER_LINE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new sales order line", description = "Creates a new sales order line and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sales order line created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<SalesOrderLineResponse> create(@Valid @RequestBody final CreateSalesOrderLineRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(salesOrderLineServiceProcessor.create(request, locale, username));
    }

    @Auditable(action = "UPDATE_SALES_ORDER_LINE")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update sales order line details", description = "Updates an existing sales order line's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sales order line updated successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order line not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<SalesOrderLineResponse> update(@Valid @RequestBody final EditSalesOrderLineRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(salesOrderLineServiceProcessor.update(request, username, locale));
    }

    @Auditable(action = "FIND_SALES_ORDER_LINE_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find sales order line by ID", description = "Retrieves a sales order line by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales order line found successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order line not found"),
            @ApiResponse(responseCode = "400", description = "Sales order line id supplied invalid")
    })
    public ResponseEntity<SalesOrderLineResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(salesOrderLineServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_SALES_ORDER_LINE")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a sales order line by ID")
    public ResponseEntity<SalesOrderLineResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(salesOrderLineServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_SALES_ORDER_LINES_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all sales order lines", description = "Retrieves a list of all sales order lines")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales order lines retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order line(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching sales order lines")
    })
    public ResponseEntity<SalesOrderLineResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(salesOrderLineServiceProcessor.findAllAsList(locale, username));
    }

    @Auditable(action = "FIND_ALL_SALES_ORDER_LINES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find sales order lines by multiple filters",
            description = "Retrieves a list of sales order lines that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales order line(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order line(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<SalesOrderLineResponse> findByMultipleFilters(@Valid @RequestBody SalesOrderLineMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(salesOrderLineServiceProcessor.findByMultipleFilters(filters, username, locale));
    }

    @Auditable(action = "EXPORT_SALES_ORDER_LINES")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export sales order lines",
            description = "Exports sales order lines based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales order lines exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody SalesOrderLineMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export sales order lines in {} format with filters: {}", format, filters);
            SalesOrderLineResponse response = salesOrderLineServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<SalesOrderLineDto> list = InventoryExportSupport.resolveExportItems(
                    response.getSalesOrderLineDtoPage(), response.getSalesOrderLineDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = salesOrderLineServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "sales_order_lines.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = salesOrderLineServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "sales_order_lines.xlsx";
                    break;
                case "pdf":
                    data = salesOrderLineServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "sales_order_lines.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export sales order lines: " + e.getMessage();
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

    @Auditable(action = "IMPORT_SALES_ORDER_LINES_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import sales order lines from CSV",
            description = "Imports sales order lines from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales order lines imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            logger.info("Incoming request by '{}' to import sales order lines from CSV file: {}", username, file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import sales order lines: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = salesOrderLineServiceProcessor.importSalesOrderLineFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import sales order lines from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import sales order lines: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
