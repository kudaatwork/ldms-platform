package projectlx.inventory.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.service.processor.api.PurchaseOrderServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.PurchaseOrderDto;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/purchase-order")
@Tag(name = "Purchase Order System Resource", description = "Operations related to managing purchase orders (system)")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderSystemResource {

    private final PurchaseOrderServiceProcessor purchaseOrderServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderSystemResource.class);

    @Auditable(action = "CREATE_PURCHASE_ORDER")
    @PostMapping("/create")
    @Operation(summary = "Create a new purchase order", description = "Creates a new purchase order and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public PurchaseOrderResponse create(@Valid @RequestBody final CreatePurchaseOrderRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseOrderServiceProcessor.create(request, locale, "SYSTEM");
    }

    @Auditable(action = "UPDATE_PURCHASE_ORDER")
    @PutMapping("/update")
    @Operation(summary = "Update purchase order details", description = "Updates an existing purchase order's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase order updated successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public PurchaseOrderResponse update(@Valid @RequestBody final EditPurchaseOrderRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return purchaseOrderServiceProcessor.update(request, "SYSTEM", locale);
    }

    @Deprecated
    @Auditable(action = "RECEIVE_GOODS")
    @PostMapping("/receive-goods")
    @Operation(summary = "⚠️ DEPRECATED - Use /purchase-orders/{id}/receive instead",
            description = "This endpoint is deprecated. Use POST /purchase-orders/{id}/receive instead.")
    public PurchaseOrderResponse receiveGoods(@Valid @RequestBody final ReceiveGoodsRequest request,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                              final Locale locale) {
        log.warn("DEPRECATED ENDPOINT CALLED: /purchase-order/receive-goods - Use /purchase-orders/{id}/receive instead");
        return purchaseOrderServiceProcessor.receiveGoods(request, "SYSTEM", locale);
    }

    @Auditable(action = "FIND_PURCHASE_ORDER_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find purchase order by ID", description = "Retrieves a purchase order by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found"),
            @ApiResponse(responseCode = "400", description = "Purchase order id supplied invalid")
    })
    public PurchaseOrderResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                   final Locale locale) {
        return purchaseOrderServiceProcessor.findById(id, locale, "SYSTEM");
    }

    @Auditable(action = "DELETE_PURCHASE_ORDER")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a purchase order by ID")
    public PurchaseOrderResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        return purchaseOrderServiceProcessor.delete(id, locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_PURCHASE_ORDERS_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all purchase orders", description = "Retrieves a list of all purchase orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase orders retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching purchase orders")
    })
    public PurchaseOrderResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                             final Locale locale) {
        return purchaseOrderServiceProcessor.findAllAsList(locale, "SYSTEM");
    }

    @Auditable(action = "FIND_ALL_PURCHASE_ORDERS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find purchase orders by multiple filters",
            description = "Retrieves a list of purchase orders that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public PurchaseOrderResponse findByMultipleFilters(@Valid @RequestBody PurchaseOrderMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                               final Locale locale) {
        return purchaseOrderServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
    }

    @Auditable(action = "EXPORT_PURCHASE_ORDERS")
    @PostMapping("/export")
    @Operation(summary = "Export purchase orders",
            description = "Exports purchase orders based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase orders exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody PurchaseOrderMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export purchase orders in {} format with filters: {}", format, filters);
            PurchaseOrderResponse response = purchaseOrderServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale);
            List<PurchaseOrderDto> list = InventoryExportSupport.resolveExportItems(
                    response.getPurchaseOrderDtoPage(), response.getPurchaseOrderDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = purchaseOrderServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "purchase_orders.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = purchaseOrderServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "purchase_orders.xlsx";
                    break;
                case "pdf":
                    data = purchaseOrderServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "purchase_orders.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export purchase orders: " + e.getMessage();
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

    @Auditable(action = "IMPORT_PURCHASE_ORDERS_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import purchase orders from CSV",
            description = "Imports purchase orders from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase orders imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import purchase orders from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import purchase orders: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = purchaseOrderServiceProcessor.importPurchaseOrderFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import purchase orders from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import purchase orders: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
