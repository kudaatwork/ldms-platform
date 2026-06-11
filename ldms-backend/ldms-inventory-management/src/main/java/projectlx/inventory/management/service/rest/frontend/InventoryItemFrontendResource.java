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
import projectlx.inventory.management.service.processor.api.InventoryItemServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.InventoryItemDto;
import projectlx.inventory.management.utils.requests.CreateInitialStockRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryItemRequest;
import projectlx.inventory.management.utils.requests.EditInventoryItemRequest;
import projectlx.inventory.management.utils.requests.InventoryItemMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/inventory-item")
@Tag(name = "Inventory Item Frontend Resource", description = "Operations related to managing inventory items")
@RequiredArgsConstructor
public class InventoryItemFrontendResource {

    private final InventoryItemServiceProcessor inventoryItemServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(InventoryItemFrontendResource.class);

    @Auditable(action = "CREATE_INVENTORY_ITEM")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new inventory item", description = "Creates a new inventory item and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventory item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public InventoryItemResponse create(@Valid @RequestBody final CreateInventoryItemRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.create(request, locale, username);
    }

    @Auditable(action = "UPDATE_INVENTORY_ITEM")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update inventory item details", description = "Updates an existing inventory item's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventory item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public InventoryItemResponse update(@Valid @RequestBody final EditInventoryItemRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.update(request, username, locale);
    }

    @Auditable(action = "FIND_INVENTORY_ITEM_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find inventory item by ID", description = "Retrieves an inventory item by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory item found successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "400", description = "Inventory item id supplied invalid")
    })
    public InventoryItemResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "DELETE_INVENTORY_ITEM")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete an inventory item by ID")
    public InventoryItemResponse delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.delete(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_INVENTORY_ITEMS_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all inventory items", description = "Retrieves a list of all inventory items")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory items retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching inventory items")
    })
    public InventoryItemResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.findAllAsList(locale, username);
    }

    @Auditable(action = "FIND_ALL_INVENTORY_ITEMS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find inventory items by multiple filters",
            description = "Retrieves a list of inventory items that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory item(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public InventoryItemResponse findByMultipleFilters(@Valid @RequestBody InventoryItemMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.findByMultipleFilters(filters, username, locale);
    }

    @Auditable(action = "EXPORT_INVENTORY_ITEMS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export inventory items",
            description = "Exports inventory items based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory items exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody InventoryItemMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export inventory items in {} format with filters: {}", format, filters);
            InventoryItemResponse response = inventoryItemServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<InventoryItemDto> list = InventoryExportSupport.resolveExportItems(
                    response.getInventoryItemDtoPage(), response.getInventoryItemDtoList());
            if (list.isEmpty() && !response.isSuccess()) {
                throw new IllegalArgumentException(
                        response.getMessage() != null ? response.getMessage() : "Could not load inventory items for export");
            }
            switch (format.toLowerCase()) {
                case "csv":
                    data = inventoryItemServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "inventory_items.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = inventoryItemServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "inventory_items.xlsx";
                    break;
                case "pdf":
                    data = inventoryItemServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "inventory_items.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export inventory items: " + e.getMessage();
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

    @Auditable(action = "IMPORT_INVENTORY_ITEMS_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import inventory items from CSV",
            description = "Imports inventory items from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory items imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            logger.info("Incoming request by '{}' to import inventory items from CSV file: {}", username, file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import inventory items: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = inventoryItemServiceProcessor.importInventoryItemFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import inventory items from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import inventory items: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }

    @Auditable(action = "CREATE_INITIAL_STOCK")
    @PostMapping("/initial-stock")
    @Operation(summary = "Create initial stock (opening balance)",
            description = "Sets the opening balance for a product at a warehouse location. This is a one-time operation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Initial stock created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or initial stock already exists"),
            @ApiResponse(responseCode = "404", description = "Product or warehouse location not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public InventoryItemResponse createInitialStock(
            @Valid @RequestBody final CreateInitialStockRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {
        logger.info("Incoming request to create initial stock for product {} at warehouse {}",
                request.getProductId(), request.getWarehouseLocationId());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.createInitialStock(request, locale, username);
    }

    @Auditable(action = "CREATE_INITIAL_STOCK_BULK")
    @PostMapping("/initial-stock/bulk")
    @Operation(summary = "Create initial stock in bulk",
            description = "Sets opening balances for multiple products at once. Each entry is processed independently.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk initial stock processing completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public InventoryItemResponse createInitialStockBulk(
            @Valid @RequestBody final List<CreateInitialStockRequest> requests,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {
        logger.info("Incoming request to create initial stock in bulk for {} items", requests.size());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryItemServiceProcessor.createInitialStockBulk(requests, locale, username);
    }
}
