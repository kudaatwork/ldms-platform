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
import projectlx.inventory.management.service.processor.api.InventoryTransferServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.InventoryTransferDto;
import projectlx.inventory.management.utils.requests.CreateInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.EditInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.InventoryTransferMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.ApproveInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.RejectInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.StartTransitInventoryTransferRequest;
import projectlx.inventory.management.utils.responses.InventoryTransferResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/inventory-transfer")
@Tag(name = "Inventory Transfer Frontend Resource", description = "Operations related to managing inventory transfers")
@RequiredArgsConstructor
public class InventoryTransferFrontendResource {

    private final InventoryTransferServiceProcessor inventoryTransferServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(InventoryTransferFrontendResource.class);

    @Auditable(action = "CREATE_INVENTORY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    @Operation(summary = "Create a new inventory transfer", description = "Creates a new inventory transfer and returns the created details.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventory transfer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public InventoryTransferResponse create(@Valid @RequestBody final CreateInventoryTransferRequest request,
                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.create(request, locale, username);
    }

    @Auditable(action = "UPDATE_INVENTORY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    @Operation(summary = "Update inventory transfer details", description = "Updates an existing inventory transfer's details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventory transfer updated successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public InventoryTransferResponse update(@Valid @RequestBody final EditInventoryTransferRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.update(request, username, locale);
    }

    @Auditable(action = "FIND_INVENTORY_TRANSFER_BY_ID")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find inventory transfer by ID", description = "Retrieves an inventory transfer by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfer found successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer not found"),
            @ApiResponse(responseCode = "400", description = "Inventory transfer id supplied invalid")
    })
    public InventoryTransferResponse findById(@PathVariable("id") final Long id,
                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.findById(id, locale, username);
    }

    @Auditable(action = "CANCEL_INVENTORY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(value = "/cancel/{id}")
    @Operation(summary = "Cancel an inventory transfer by ID",
            description = "Cancels an inventory transfer before completion. Reverses stock movements if transfer was in transit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfer cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer not found"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel completed transfer")
    })
    public InventoryTransferResponse cancel(@PathVariable("id") final Long id,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.cancel(id, locale, username);
    }

    @Auditable(action = "FIND_ALL_INVENTORY_TRANSFERS_BY_LIST")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all inventory transfers", description = "Retrieves a list of all inventory transfers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfers retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching inventory transfers")
    })
    public InventoryTransferResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.findAllAsList(locale, username);
    }

    @Auditable(action = "APPROVE_INVENTORY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/approve")
    @Operation(summary = "Approve an inventory transfer", description = "Approves an inventory transfer before it can start transit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfer approved successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public InventoryTransferResponse approve(@Valid @RequestBody final ApproveInventoryTransferRequest request,
                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                  final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.approveTransfer(
                request.getTransferId(),
                request.getApprovedByUserId(),
                locale,
                username
        );
    }

    @Auditable(action = "REJECT_INVENTORY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reject")
    @Operation(summary = "Reject an inventory transfer", description = "Rejects a requested inventory transfer with a reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfer rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public InventoryTransferResponse reject(@Valid @RequestBody final RejectInventoryTransferRequest request,
                                 @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                 @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                 final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.rejectTransfer(
                request.getTransferId(),
                request.getRejectedByUserId(),
                request.getRejectionReason(),
                locale,
                username
        );
    }

    @Auditable(action = "START_TRANSIT_INVENTORY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/start-transit")
    @Operation(summary = "Start transit for an inventory transfer", description = "Marks an approved inventory transfer as in transit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfer transit started successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public InventoryTransferResponse startTransit(@Valid @RequestBody final StartTransitInventoryTransferRequest request,
                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                  final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.startTransit(
                request.getTransferId(),
                request.getStartedByUserId(),
                request.getTripId(),
                request.getShipmentId(),
                locale,
                username
        );
    }

    @Auditable(action = "COMPLETE_INVENTORY_TRANSFER")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/complete")
    @Operation(summary = "Complete an inventory transfer", description = "Completes an inventory transfer and updates stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfer completed successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public InventoryTransferResponse complete(@Valid @RequestBody final projectlx.inventory.management.utils.requests.CompleteInventoryTransferRequest request,
                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                  final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.completeTransfer(
                request.getTransferId(),
                request.getUpdatedByUserId(),
                request.getIdempotencyKey(),
                locale,
                username
        );
    }

    @Auditable(action = "FIND_ALL_INVENTORY_TRANSFERS_BY_MULTIPLE_FILTERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find inventory transfers by multiple filters",
            description = "Retrieves a list of inventory transfers that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfer(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory transfer(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public InventoryTransferResponse findByMultipleFilters(@Valid @RequestBody InventoryTransferMultipleFiltersRequest filters,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return inventoryTransferServiceProcessor.findByMultipleFilters(filters, username, locale);
    }

    @Auditable(action = "EXPORT_INVENTORY_TRANSFERS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/export")
    @Operation(summary = "Export inventory transfers",
            description = "Exports inventory transfers based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfers exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody InventoryTransferMultipleFiltersRequest filters,
                                              @RequestParam String format,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export inventory transfers in {} format with filters: {}", format, filters);
            InventoryTransferResponse response = inventoryTransferServiceProcessor.findByMultipleFilters(filters, username, locale);
            List<InventoryTransferDto> list = InventoryExportSupport.resolveExportItems(
                    response.getInventoryTransferDtoPage(), response.getInventoryTransferDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = inventoryTransferServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "inventory_transfers.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = inventoryTransferServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "inventory_transfers.xlsx";
                    break;
                case "pdf":
                    data = inventoryTransferServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "inventory_transfers.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export inventory transfers: " + e.getMessage();
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

    @Auditable(action = "IMPORT_INVENTORY_TRANSFERS_FROM_CSV")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/import-csv")
    @Operation(summary = "Import inventory transfers from CSV",
            description = "Imports inventory transfers from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory transfers imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            logger.info("Incoming request by '{}' to import inventory transfers from CSV file: {}", username, file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import inventory transfers: Empty file", 0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = inventoryTransferServiceProcessor.importInventoryTransferFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import inventory transfers from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import inventory transfers: " + e.getMessage(), 0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
