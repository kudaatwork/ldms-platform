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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.service.processor.api.ProcurementWorkflowServiceProcessor;
import projectlx.inventory.management.utils.requests.*;
import projectlx.inventory.management.utils.responses.*;
import projectlx.inventory.management.utils.security.ProcurementWorkflowRoles;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/procurement-workflow")
@Tag(name = "Procurement Workflow", description = "Operations for the procurement lifecycle: PR approval, supplier quoting, PO approval, and SO approval")
@RequiredArgsConstructor
public class ProcurementWorkflowFrontendResource {

    private final ProcurementWorkflowServiceProcessor procurementWorkflowServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(ProcurementWorkflowFrontendResource.class);

    // ============================================================
    // PR INTERNAL APPROVAL
    // ============================================================

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/pr/approve-stage")
    @Operation(summary = "Approve internal PR stage",
            description = "Approves an internal approval stage on a purchase requisition. " +
                    "When all required stages are complete, PR transitions to APPROVED. " +
                    "If a preferred supplier is set, PR is auto-published to the supplier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stage approved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or wrong status"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public PurchaseRequisitionResponse approveInternalStage(
            @Valid @RequestBody ApprovePurchaseRequisitionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Approving internal PR stage: {} by user: {}", request.getId(), username);
        return procurementWorkflowServiceProcessor.approveInternalStage(request, locale, username);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/pr/{requisitionId}/publish-to-supplier")
    @Operation(summary = "Publish PR to supplier",
            description = "Manually publishes an APPROVED purchase requisition to the preferred supplier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PR published to supplier"),
            @ApiResponse(responseCode = "400", description = "PR not in APPROVED status"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found")
    })
    public PurchaseRequisitionResponse publishToSupplier(
            @PathVariable Long requisitionId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Publishing PR {} to supplier by user: {}", requisitionId, username);
        return procurementWorkflowServiceProcessor.publishToSupplier(requisitionId, locale, username);
    }

    // ============================================================
    // SUPPLIER QUOTING
    // ============================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/pr/supplier-visible")
    @Operation(summary = "Get supplier-visible requisitions",
            description = "Returns all purchase requisitions published to the calling supplier organisation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requisitions retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public PurchaseRequisitionResponse findSupplierVisibleRequisitions(
            @RequestParam Long supplierOrganizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Finding supplier-visible PRs for supplier org: {}", supplierOrganizationId);
        return procurementWorkflowServiceProcessor.findSupplierVisibleRequisitions(supplierOrganizationId, locale, username);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/pr/submit-quote")
    @Operation(summary = "Submit supplier quote",
            description = "Supplier submits a quote against a published PR. Sets PR status to SUPPLIER_CONFIRMED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Quote submitted successfully"),
            @ApiResponse(responseCode = "400", description = "PR not in PUBLISHED_TO_SUPPLIER status"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found")
    })
    public SupplierQuoteResponse submitSupplierQuote(
            @Valid @RequestBody SubmitSupplierQuoteRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Submitting supplier quote for PR: {} by user: {}", request.getPurchaseRequisitionId(), username);
        return procurementWorkflowServiceProcessor.submitSupplierQuote(request, locale, username);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/pr/acknowledge-quote")
    @Operation(summary = "Acknowledge supplier quote",
            description = "Customer acknowledges the supplier quote. Sets PR status to CUSTOMER_ACKNOWLEDGED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quote acknowledged successfully"),
            @ApiResponse(responseCode = "400", description = "PR not in SUPPLIER_CONFIRMED status"),
            @ApiResponse(responseCode = "404", description = "Purchase requisition not found")
    })
    public PurchaseRequisitionResponse acknowledgeSupplierQuote(
            @Valid @RequestBody AcknowledgeSupplierQuoteRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Acknowledging supplier quote for PR: {} by user: {}", request.getPurchaseRequisitionId(), username);
        return procurementWorkflowServiceProcessor.acknowledgeSupplierQuote(request, locale, username);
    }

    // ============================================================
    // PO APPROVAL
    // ============================================================

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/po/approve-customer-stage")
    @Operation(summary = "Approve PO customer stage",
            description = "Customer approves a stage on a purchase order. " +
                    "On final stage: PO moves to CUSTOMER_APPROVED then PENDING_SUPPLIER_APPROVAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer stage approved"),
            @ApiResponse(responseCode = "400", description = "PO not in expected status"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public PurchaseOrderResponse approvePurchaseOrderCustomerStage(
            @Valid @RequestBody ApprovePurchaseOrderStageRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Approving PO customer stage for PO: {} by user: {}", request.getPurchaseOrderId(), username);
        return procurementWorkflowServiceProcessor.approvePurchaseOrderCustomerStage(request, locale, username);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/po/approve-supplier-stage")
    @Operation(summary = "Approve PO supplier stage",
            description = "Supplier approves a stage on a purchase order. " +
                    "On final stage: PO transitions to APPROVED and a po.approved RabbitMQ event is published.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supplier stage approved"),
            @ApiResponse(responseCode = "400", description = "PO not in PENDING_SUPPLIER_APPROVAL status"),
            @ApiResponse(responseCode = "404", description = "Purchase order not found")
    })
    public PurchaseOrderResponse approvePurchaseOrderSupplierStage(
            @Valid @RequestBody ApprovePurchaseOrderStageRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Approving PO supplier stage for PO: {} by user: {}", request.getPurchaseOrderId(), username);
        return procurementWorkflowServiceProcessor.approvePurchaseOrderSupplierStage(request, locale, username);
    }

    // ============================================================
    // SO APPROVAL
    // ============================================================

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/so/approve-stage")
    @Operation(summary = "Approve SO stage",
            description = "Approves an approval stage on a sales order. " +
                    "On final stage: SO transitions to APPROVED, ready for shipment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SO stage approved"),
            @ApiResponse(responseCode = "400", description = "SO not in PENDING_APPROVAL status"),
            @ApiResponse(responseCode = "404", description = "Sales order not found")
    })
    public SalesOrderResponse approveSalesOrderStage(
            @Valid @RequestBody ApproveSalesOrderStageRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Approving SO stage for SO: {} by user: {}", request.getSalesOrderId(), username);
        return procurementWorkflowServiceProcessor.approveSalesOrderStage(request, locale, username);
    }

    // ============================================================
    // QUOTE LISTING
    // ============================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/quotes/by-supplier")
    @Operation(summary = "List quotes by supplier organisation",
            description = "Returns all supplier quotes submitted by the given supplier organisation, ordered newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quotes retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public SupplierQuoteResponse findSupplierQuotes(
            @Parameter(description = "Supplier organisation ID") @RequestParam Long supplierOrganizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Finding quotes for supplier org: {} by user: {}", supplierOrganizationId, username);
        return procurementWorkflowServiceProcessor.findSupplierQuotes(supplierOrganizationId, locale, username);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/quotes/by-customer")
    @Operation(summary = "List quotes by customer organisation",
            description = "Returns all supplier quotes received by the given customer organisation, ordered newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quotes retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public SupplierQuoteResponse findCustomerQuotes(
            @Parameter(description = "Customer organisation ID") @RequestParam Long customerOrganizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Finding quotes for customer org: {} by user: {}", customerOrganizationId, username);
        return procurementWorkflowServiceProcessor.findCustomerQuotes(customerOrganizationId, locale, username);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/so/by-customer")
    @Operation(summary = "List sales orders by customer organisation",
            description = "Returns all sales orders linked to purchase orders for the given customer organisation, ordered newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public SalesOrderResponse findCustomerSalesOrders(
            @Parameter(description = "Customer organisation ID") @RequestParam Long customerOrganizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Finding sales orders for customer org: {} by user: {}", customerOrganizationId, username);
        return procurementWorkflowServiceProcessor.findCustomerSalesOrders(customerOrganizationId, locale, username);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/quotes/by-requisition/{requisitionId}")
    @Operation(summary = "Get latest quote for a requisition",
            description = "Returns the most recent supplier quote for the given purchase requisition, including all quote lines.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quote retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Requisition or quote not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public SupplierQuoteResponse findQuoteByRequisitionId(
            @Parameter(description = "Purchase requisition ID") @PathVariable Long requisitionId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Finding latest quote for requisition: {} by user: {}", requisitionId, username);
        return procurementWorkflowServiceProcessor.findQuoteByRequisitionId(requisitionId, locale, username);
    }
}
