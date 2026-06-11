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
import projectlx.inventory.management.service.processor.api.PurchaseOrderLineServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.PurchaseOrderLineDto;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderLineMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderLineResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/purchase-order-line")
@Tag(name = "Purchase Order Line Frontend Resource", description = "Operations related to managing purchase order lines")
@RequiredArgsConstructor
public class PurchaseOrderLineFrontendResource {

    private final PurchaseOrderLineServiceProcessor purchaseOrderLineServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderLineFrontendResource.class);

    @Auditable(action = "CREATE_PURCHASE_ORDER_LINE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new purchase order line", description = "Creates a new purchase order line and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase order line created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<PurchaseOrderLineResponse> create(@Valid @RequestBody final CreatePurchaseOrderLineRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(purchaseOrderLineServiceProcessor.create(request, locale, username));
    }

    @Auditable(action = "UPDATE_PURCHASE_ORDER_LINE")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update purchase order line details", description = "Updates an existing purchase order line's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase order line updated successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order line not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PurchaseOrderLineResponse> update(@Valid @RequestBody final EditPurchaseOrderLineRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(purchaseOrderLineServiceProcessor.update(request, username, locale));
    }

    @Auditable(action = "FIND_PURCHASE_ORDER_LINE_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find purchase order line by ID", description = "Retrieves a purchase order line by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order line found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order line not found"),
            @ApiResponse(responseCode = "400", description = "Purchase order line id supplied invalid")
    })
    public ResponseEntity<PurchaseOrderLineResponse> findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(purchaseOrderLineServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_PURCHASE_ORDER_LINE")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a purchase order line by ID")
    public ResponseEntity<PurchaseOrderLineResponse> delete(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(purchaseOrderLineServiceProcessor.delete(id, locale, username));
    }

    @Auditable(action = "FIND_ALL_PURCHASE_ORDER_LINES_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all purchase order lines", description = "Retrieves a list of all purchase order lines")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order lines retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order line(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching purchase order lines")
    })
    public ResponseEntity<PurchaseOrderLineResponse> findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(purchaseOrderLineServiceProcessor.findAllAsList(locale, username));
    }

    @Auditable(action = "FIND_ALL_PURCHASE_ORDER_LINES_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find purchase order lines by multiple filters",
            description = "Retrieves a list of purchase order lines that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order line(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase order line(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public ResponseEntity<PurchaseOrderLineResponse> findByMultipleFilters(@Valid @RequestBody PurchaseOrderLineMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(purchaseOrderLineServiceProcessor.findByMultipleFilters(filters, username, locale));
    }

    @Auditable(action = "EXPORT_PURCHASE_ORDER_LINES")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export purchase order lines",
            description = "Exports purchase order lines based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order lines exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody PurchaseOrderLineMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export purchase order lines in {} format with filters: {}", format, filters);
            PurchaseOrderLineResponse response = purchaseOrderLineServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<PurchaseOrderLineDto> list = InventoryExportSupport.resolveExportItems(
                    response.getPurchaseOrderLineDtoPage(), response.getPurchaseOrderLineDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = purchaseOrderLineServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "purchase_order_lines.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = purchaseOrderLineServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "purchase_order_lines.xlsx";
                    break;
                case "pdf":
                    data = purchaseOrderLineServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "purchase_order_lines.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export purchase order lines: " + e.getMessage();
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

    @Auditable(action = "IMPORT_PURCHASE_ORDER_LINES_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import purchase order lines from CSV",
            description = "Imports purchase order lines from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase order lines imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            logger.info("Incoming request by '{}' to import purchase order lines from CSV file: {}", username, file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import purchase order lines: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = purchaseOrderLineServiceProcessor.importPurchaseOrderLineFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import purchase order lines from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import purchase order lines: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
