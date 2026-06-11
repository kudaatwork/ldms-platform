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
import projectlx.inventory.management.service.processor.api.SalesOrderServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.SalesOrderDto;
import projectlx.inventory.management.utils.requests.CreateSalesOrderRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderRequest;
import projectlx.inventory.management.utils.requests.FulfillSalesOrderRequest;
import projectlx.inventory.management.utils.requests.SalesOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/sales-order")
@Tag(name = "Sales Order Frontend Resource", description = "Operations related to managing sales orders")
@RequiredArgsConstructor
public class SalesOrderFrontendResource {

    private final SalesOrderServiceProcessor salesOrderServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(SalesOrderFrontendResource.class);

    @Auditable(action = "CREATE_SALES_ORDER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new sales order", description = "Creates a new sales order and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sales order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public SalesOrderResponse create(@Valid @RequestBody final CreateSalesOrderRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return salesOrderServiceProcessor.create(request, locale, username);
    }

    @Auditable(action = "UPDATE_SALES_ORDER")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update sales order details", description = """
        Updates sales order. 
        
        ⚠️ IMPORTANT: Status transitions trigger business logic:
        - PENDING → CONFIRMED: Reserves stock at fulfillmentWarehouseId
        - CONFIRMED → CANCELLED: Releases reserved stock
        
        Required for confirmation:
        - status: "CONFIRMED"
        - fulfillmentWarehouseId: Warehouse to fulfill from
    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sales order updated successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public SalesOrderResponse update(@Valid @RequestBody final EditSalesOrderRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return salesOrderServiceProcessor.update(request, username, locale);
    }

    @Auditable(action = "FULFILL_SALES_ORDER")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/fulfill")
    @Operation(summary = "Fulfill a sales order", description = "Processes fulfillment of a sales order and its line items.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sales order fulfilled successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public SalesOrderResponse fulfill(@Valid @RequestBody final FulfillSalesOrderRequest request,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                   final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return salesOrderServiceProcessor.fulfillOrder(request, username, locale);
    }

    @Auditable(action = "FIND_SALES_ORDER_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find sales order by ID", description = "Retrieves a sales order by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales order found successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order not found"),
            @ApiResponse(responseCode = "400", description = "Sales order id supplied invalid")
    })
    public SalesOrderResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return salesOrderServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_SALES_ORDER")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a sales order by ID")
    public SalesOrderResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return salesOrderServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_SALES_ORDERS_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all sales orders", description = "Retrieves a list of all sales orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales orders retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching sales orders")
    })
    public SalesOrderResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return salesOrderServiceProcessor.findAllAsList(locale, username);
    }

    @Auditable(action = "FIND_ALL_SALES_ORDERS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find sales orders by multiple filters",
            description = "Retrieves a list of sales orders that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales order(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Sales order(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public SalesOrderResponse findByMultipleFilters(@Valid @RequestBody SalesOrderMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return salesOrderServiceProcessor.findByMultipleFilters(filters, username, locale);
    }

    @Auditable(action = "EXPORT_SALES_ORDERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export sales orders",
            description = "Exports sales orders based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales orders exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody SalesOrderMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export sales orders in {} format with filters: {}", format, filters);
            SalesOrderResponse response = salesOrderServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<SalesOrderDto> list = InventoryExportSupport.resolveExportItems(
                    response.getSalesOrderDtoPage(), response.getSalesOrderDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = salesOrderServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "sales_orders.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = salesOrderServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "sales_orders.xlsx";
                    break;
                case "pdf":
                    data = salesOrderServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "sales_orders.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export sales orders: " + e.getMessage();
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

    @Auditable(action = "IMPORT_SALES_ORDERS_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import sales orders from CSV",
            description = "Imports sales orders from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales orders imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            logger.info("Incoming request by '{}' to import sales orders from CSV file: {}", username, file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import sales orders: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = salesOrderServiceProcessor.importSalesOrderFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import sales orders from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import sales orders: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
