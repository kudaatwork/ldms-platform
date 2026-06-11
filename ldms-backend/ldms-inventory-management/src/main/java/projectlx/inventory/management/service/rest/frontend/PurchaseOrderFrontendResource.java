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
@RequestMapping("/ldms-inventory-management/v1/frontend/purchase-order")
@Tag(name = "Purchase Order Frontend Resource", description = "Operations related to managing purchase orders")
@RequiredArgsConstructor
public class PurchaseOrderFrontendResource {

    private final PurchaseOrderServiceProcessor purchaseOrderServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderFrontendResource.class);

    @Auditable(action = "CREATE_PURCHASE_ORDER")
    @PreAuthorize("isAuthenticated()")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return purchaseOrderServiceProcessor.create(request, locale, username);
    }

    @Auditable(action = "UPDATE_PURCHASE_ORDER")
    @PreAuthorize("isAuthenticated()")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return purchaseOrderServiceProcessor.update(request, username, locale);
    }

    @Auditable(action = "RECEIVE_GOODS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/receive-goods")
    @Operation(summary = "Receive goods for a purchase order", description = "Receives goods against a purchase order and updates stock and GRV accordingly.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Goods received successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public PurchaseOrderResponse receiveGoods(@Valid @RequestBody final ReceiveGoodsRequest request,
                                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                              final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return purchaseOrderServiceProcessor.receiveGoods(request, username, locale);
    }

    @Auditable(action = "FIND_PURCHASE_ORDER_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find purchase order by ID", description = "Retrieves a purchase order by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found"),
            @ApiResponse(responseCode = "400", description = "Purchase order id supplied invalid")
    })
    public PurchaseOrderResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return purchaseOrderServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_PURCHASE_ORDER")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a purchase order by ID")
    public PurchaseOrderResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return purchaseOrderServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_PURCHASE_ORDERS_BY_LIST")
    @PreAuthorize("isAuthenticated()")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return purchaseOrderServiceProcessor.findAllAsList(locale, username);
    }

    @Auditable(action = "FIND_ALL_PURCHASE_ORDERS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return purchaseOrderServiceProcessor.findByMultipleFilters(filters, username, locale);
    }

    @Auditable(action = "EXPORT_PURCHASE_ORDERS")
    @PreAuthorize("isAuthenticated()")
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
            PurchaseOrderResponse response = purchaseOrderServiceProcessor.findByMultipleFilters(filters, SecurityContextHolder.getContext().getAuthentication().getName(), locale);
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
    @PreAuthorize("isAuthenticated()")
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
