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
import projectlx.inventory.management.service.processor.api.PurchaseRequisitionServiceProcessor;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.PurchaseRequisitionDto;
import projectlx.inventory.management.utils.requests.ApprovePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CancelPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePOFromPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.FulfillPurchaseRequisitionLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseRequisitionMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.RejectPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.inventory.management.utils.responses.PurchaseRequisitionResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/purchase-requisition")
@Tag(name = "Purchase Requisition System Resource", description = "System-level operations for managing purchase requisitions (no auth required)")
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequisitionSystemResource {

    private final PurchaseRequisitionServiceProcessor purchaseRequisitionServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(PurchaseRequisitionSystemResource.class);
    private static final String SYSTEM_USER = "SYSTEM";

    // === CRUD OPERATIONS ===

    @Auditable(action = "CREATE_PURCHASE_REQUISITION")
    @PostMapping("/create")
    @Operation(summary = "Create a new purchase requisition (system)",
            description = "Creates a new purchase requisition in DRAFT status.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase requisition created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public PurchaseRequisitionResponse create(@Valid @RequestBody final CreatePurchaseRequisitionRequest request,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.create(request, locale, SYSTEM_USER);
    }

    @Auditable(action = "UPDATE_PURCHASE_REQUISITION")
    @PutMapping("/update")
    @Operation(summary = "Update purchase requisition details (system)",
            description = "Updates an existing purchase requisition (only allowed in DRAFT status).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition updated successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or PR not in DRAFT status")
    })
    public PurchaseRequisitionResponse update(@Valid @RequestBody final EditPurchaseRequisitionRequest request,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.update(request, SYSTEM_USER, locale);
    }

    @Auditable(action = "FIND_PURCHASE_REQUISITION_BY_ID")
    @GetMapping(value = "/find-by-id/{id}")
    @Operation(summary = "Find purchase requisition by ID (system)",
            description = "Retrieves a purchase requisition by its unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Purchase requisition ID supplied invalid")
    })
    public PurchaseRequisitionResponse findById(@PathVariable("id") final Long id,
                                                @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                        defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.findById(id, locale, SYSTEM_USER);
    }

    @Auditable(action = "DELETE_PURCHASE_REQUISITION")
    @DeleteMapping(value = "/delete-by-id/{id}")
    @Operation(summary = "Delete a purchase requisition by ID (system)",
            description = "Soft-deletes a purchase requisition (only allowed in DRAFT status).")
    public PurchaseRequisitionResponse delete(@PathVariable("id") final Long id,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.delete(id, locale, SYSTEM_USER);
    }

    @Auditable(action = "FIND_ALL_PURCHASE_REQUISITIONS_BY_LIST")
    @GetMapping(value = "/find-by-list")
    @Operation(summary = "Get all purchase requisitions (system)",
            description = "Retrieves a list of all purchase requisitions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisitions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition(s) not found"),
            @ApiResponse(responseCode = "500", description = "Server error while fetching purchase requisitions")
    })
    public PurchaseRequisitionResponse findAllAsAList(@Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.findAllAsList(locale, SYSTEM_USER);
    }

    @Auditable(action = "FIND_ALL_PURCHASE_REQUISITIONS_BY_MULTIPLE_FILTERS")
    @PostMapping(value = "/find-by-multiple-filters")
    @Operation(summary = "Find purchase requisitions by multiple filters (system)",
            description = "Retrieves a paginated list of purchase requisitions that match the provided filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition(s) found successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition(s) not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filter criteria")
    })
    public PurchaseRequisitionResponse findByMultipleFilters(@Valid @RequestBody PurchaseRequisitionMultipleFiltersRequest filters,
                                                             @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                             @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                     defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.findByMultipleFilters(filters, SYSTEM_USER, locale);
    }

    // === WORKFLOW OPERATIONS ===

    @Auditable(action = "SUBMIT_PURCHASE_REQUISITION")
    @PostMapping(value = "/submit/{id}")
    @Operation(summary = "Submit a purchase requisition for approval (system)",
            description = "Transitions a purchase requisition from DRAFT to SUBMITTED status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition submitted successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Purchase requisition cannot be submitted")
    })
    public PurchaseRequisitionResponse submit(@PathVariable("id") final Long id,
                                              @RequestParam("submittedByUserId") final Long submittedByUserId,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.submit(id, submittedByUserId, locale, SYSTEM_USER);
    }

    @Auditable(action = "APPROVE_PURCHASE_REQUISITION")
    @PostMapping(value = "/approve")
    @Operation(summary = "Approve a purchase requisition (system)",
            description = "Approves a submitted purchase requisition. Optionally allows adjusting approved quantities per line.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition approved successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Purchase requisition cannot be approved")
    })
    public PurchaseRequisitionResponse approve(@Valid @RequestBody final ApprovePurchaseRequisitionRequest request,
                                               @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                               @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                       defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.approve(request, locale, SYSTEM_USER);
    }

    @Auditable(action = "REJECT_PURCHASE_REQUISITION")
    @PostMapping(value = "/reject")
    @Operation(summary = "Reject a purchase requisition (system)",
            description = "Rejects a submitted purchase requisition with a mandatory reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Purchase requisition cannot be rejected")
    })
    public PurchaseRequisitionResponse reject(@Valid @RequestBody final RejectPurchaseRequisitionRequest request,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.reject(request, locale, SYSTEM_USER);
    }

    @Auditable(action = "CANCEL_PURCHASE_REQUISITION")
    @PostMapping(value = "/cancel")
    @Operation(summary = "Cancel a purchase requisition (system)",
            description = "Cancels a purchase requisition with a mandatory reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Purchase requisition cannot be cancelled")
    })
    public PurchaseRequisitionResponse cancel(@Valid @RequestBody final CancelPurchaseRequisitionRequest request,
                                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.cancel(request, locale, SYSTEM_USER);
    }

    @Auditable(action = "CLOSE_PURCHASE_REQUISITION")
    @PostMapping(value = "/close/{id}")
    @Operation(summary = "Close a purchase requisition (system)",
            description = "Administratively closes a purchase requisition. Used for partially fulfilled PRs that won't be completed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisition closed successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Purchase requisition cannot be closed")
    })
    public PurchaseRequisitionResponse close(@PathVariable("id") final Long id,
                                             @RequestParam("closedByUserId") final Long closedByUserId,
                                             @RequestParam(value = "reason", required = false) final String reason,
                                             @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                             @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                     defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.close(id, closedByUserId, reason, locale, SYSTEM_USER);
    }

    // === FULFILLMENT OPERATIONS ===

    @Auditable(action = "FULFILL_PURCHASE_REQUISITION_LINE")
    @PostMapping(value = "/fulfill-line")
    @Operation(summary = "Record fulfillment for a PR line (system)",
            description = "Records fulfillment for a specific line item. Supports PURCHASE, FROM_STOCK, TRANSFER, DEFERRED, NOT_REQUIRED methods.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Line fulfilled successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition or line not found"),
            @ApiResponse(responseCode = "400", description = "Invalid fulfillment request")
    })
    public PurchaseRequisitionResponse fulfillLine(@Valid @RequestBody final FulfillPurchaseRequisitionLineRequest request,
                                                   @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                   @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                           defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.fulfillLine(request, locale, SYSTEM_USER);
    }

    // === PO CONVERSION ===

    @Auditable(action = "CREATE_PO_FROM_PURCHASE_REQUISITION")
    @PostMapping(value = "/create-po")
    @Operation(summary = "Create Purchase Order from Purchase Requisition (system)",
            description = "Creates a new PO from an approved PR. Only lines with fulfillment_method=PURCHASE and remaining_quantity > 0 are eligible.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Purchase order created successfully"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request or no eligible lines for PO creation")
    })
    public PurchaseOrderResponse createPurchaseOrderFromPR(@Valid @RequestBody final CreatePOFromPurchaseRequisitionRequest request,
                                                           @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                           @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                   defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.createPurchaseOrderFromPR(request, locale, SYSTEM_USER);
    }

    // === UTILITY OPERATIONS ===

    @Auditable(action = "FIND_PURCHASE_REQUISITIONS_BY_DEPARTMENT")
    @GetMapping(value = "/find-by-department/{departmentId}")
    @Operation(summary = "Find purchase requisitions by department (system)",
            description = "Retrieves all purchase requisitions for a specific department.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisitions found successfully"),
            @ApiResponse(responseCode = "404", description = "No purchase requisitions found for department")
    })
    public PurchaseRequisitionResponse findByDepartment(@PathVariable("departmentId") final Long departmentId,
                                                        @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                        @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.findByDepartment(departmentId, locale, SYSTEM_USER);
    }

    @Auditable(action = "FIND_PENDING_APPROVAL_PURCHASE_REQUISITIONS")
    @GetMapping(value = "/pending-approvals")
    @Operation(summary = "Find purchase requisitions pending approval (system)",
            description = "Retrieves all purchase requisitions in SUBMITTED status awaiting approval.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending purchase requisitions found successfully"),
            @ApiResponse(responseCode = "404", description = "No pending purchase requisitions found")
    })
    public PurchaseRequisitionResponse findPendingApprovals(@RequestParam(value = "departmentId", required = false) final Long departmentId,
                                                            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.findPendingApprovals(departmentId, locale, SYSTEM_USER);
    }

    @Auditable(action = "FIND_APPROVED_PENDING_FULFILLMENT_PURCHASE_REQUISITIONS")
    @GetMapping(value = "/approved-pending-fulfillment")
    @Operation(summary = "Find approved purchase requisitions pending fulfillment (system)",
            description = "Retrieves all approved purchase requisitions that still have unfulfilled lines.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisitions found successfully"),
            @ApiResponse(responseCode = "404", description = "No pending fulfillment purchase requisitions found")
    })
    public PurchaseRequisitionResponse findApprovedPendingFulfillment(@RequestParam(value = "organizationId", required = false) final Long organizationId,
                                                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                                              defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return purchaseRequisitionServiceProcessor.findApprovedPendingFulfillment(organizationId, locale, SYSTEM_USER);
    }

    // === SCHEDULED TASK TRIGGER ===

    @Auditable(action = "EXPIRE_OVERDUE_PURCHASE_REQUISITIONS")
    @PostMapping(value = "/expire-overdue")
    @Operation(summary = "Trigger expiration of overdue purchase requisitions (system)",
            description = "Manually triggers the expiration process for overdue purchase requisitions. Normally runs on a schedule.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expiration process triggered successfully")
    })
    public ResponseEntity<String> expireOverdueRequisitions() {
        logger.info("Manually triggering expiration of overdue purchase requisitions");
        purchaseRequisitionServiceProcessor.expireOverdueRequisitions();
        return ResponseEntity.ok("Expiration process triggered successfully");
    }

    // === EXPORT OPERATIONS ===

    @Auditable(action = "EXPORT_PURCHASE_REQUISITIONS")
    @PostMapping("/export")
    @Operation(summary = "Export purchase requisitions (system)",
            description = "Exports purchase requisitions based on filters in the specified format (csv, excel, pdf).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisitions exported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> export(@RequestBody PurchaseRequisitionMultipleFiltersRequest filters,
                                         @RequestParam String format,
                                         @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                         @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                 defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        byte[] data;
        String contentType;
        String filename;
        try {
            logger.info("Incoming request to export purchase requisitions in {} format with filters: {}", format, filters);
            PurchaseRequisitionResponse response = purchaseRequisitionServiceProcessor.findByMultipleFilters(filters, SYSTEM_USER, locale);
            List<PurchaseRequisitionDto> list = InventoryExportSupport.resolveExportItems(
                    response.getPurchaseRequisitionDtoPage(), response.getPurchaseRequisitionDtoList());
            switch (format.toLowerCase()) {
                case "csv":
                    data = purchaseRequisitionServiceProcessor.exportToCsv(list);
                    contentType = "text/csv";
                    filename = "purchase_requisitions.csv";
                    break;
                case "excel":
                case "xlsx":
                    data = purchaseRequisitionServiceProcessor.exportToExcel(list);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename = "purchase_requisitions.xlsx";
                    break;
                case "pdf":
                    data = purchaseRequisitionServiceProcessor.exportToPdf(list);
                    contentType = "application/pdf";
                    filename = "purchase_requisitions.pdf";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to export purchase requisitions: " + e.getMessage();
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

    @Auditable(action = "IMPORT_PURCHASE_REQUISITIONS_FROM_CSV")
    @PostMapping("/import-csv")
    @Operation(summary = "Import purchase requisitions from CSV (system)",
            description = "Imports purchase requisitions from a CSV file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase requisitions imported successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid CSV file or import failed"),
            @ApiResponse(responseCode = "500", description = "Error during import")
    })
    public ResponseEntity<ImportSummary> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Incoming request to import purchase requisitions from CSV file: {}", file.getOriginalFilename());
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportSummary(400, false, "Failed to import purchase requisitions: Empty file",
                                0, 0, 0, List.of("Empty file provided"))
                );
            }
            try (InputStream inputStream = file.getInputStream()) {
                ImportSummary summary = purchaseRequisitionServiceProcessor.importPurchaseRequisitionFromCsv(inputStream);
                return ResponseEntity.ok(summary);
            }
        } catch (IOException e) {
            logger.error("Failed to import purchase requisitions from CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportSummary(500, false, "Failed to import purchase requisitions: " + e.getMessage(),
                            0, 0, 0, List.of(e.getMessage()))
            );
        }
    }
}
