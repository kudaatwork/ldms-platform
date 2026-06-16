package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.ProcurementWorkflowService;
import projectlx.inventory.management.business.logic.api.SalesOrderCreationService;
import projectlx.inventory.management.business.logic.support.PlatformWalletUsageSupport;
import projectlx.inventory.management.business.logic.support.ProcurementApprovalService;
import projectlx.inventory.management.business.logic.support.ProcurementApprovalService.StageApprovalResult;
import projectlx.inventory.management.business.logic.support.ProcurementApprovalStageResolver;
import projectlx.inventory.management.business.logic.support.ProcurementApproverSupport;
import projectlx.inventory.management.business.logic.support.SupplierQuoteSubmissionValidator;
import projectlx.inventory.management.model.*;
import projectlx.inventory.management.repository.*;
import projectlx.inventory.management.utils.NumberGenerator;
import projectlx.inventory.management.utils.dtos.PurchaseOrderDto;
import projectlx.inventory.management.utils.dtos.PurchaseRequisitionDto;
import projectlx.inventory.management.utils.dtos.SalesOrderDto;
import projectlx.inventory.management.utils.dtos.SupplierQuoteDto;
import projectlx.inventory.management.utils.dtos.SupplierQuoteLineDto;
import projectlx.inventory.management.utils.requests.*;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.inventory.management.utils.responses.PurchaseRequisitionResponse;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;
import projectlx.inventory.management.utils.responses.SupplierQuoteResponse;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Procurement Workflow Service Implementation
 *
 * Orchestrates the complete procurement workflow from internal PR approval
 * through supplier quoting, PO approval (customer + supplier stages), and
 * Sales Order management.
 *
 * WORKFLOW STAGES:
 * 1. PR INTERNAL APPROVAL: SUBMITTED → (multi-stage) → APPROVED → PUBLISHED_TO_SUPPLIER
 * 2. SUPPLIER QUOTING:     PUBLISHED_TO_SUPPLIER → SUPPLIER_CONFIRMED → CUSTOMER_ACKNOWLEDGED
 * 3. PO CUSTOMER APPROVAL: SUBMITTED → PENDING_CUSTOMER_APPROVAL → CUSTOMER_APPROVED → PENDING_SUPPLIER_APPROVAL
 * 4. PO SUPPLIER APPROVAL: PENDING_SUPPLIER_APPROVAL → APPROVED (publishes po.approved RabbitMQ event)
 * 5. SO APPROVAL:          PENDING_APPROVAL → (multi-stage) → APPROVED
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProcurementWorkflowServiceImpl implements ProcurementWorkflowService {

    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SupplierQuoteRepository supplierQuoteRepository;
    private final ProcurementApprovalService procurementApprovalService;
    private final ProcurementApprovalStageResolver approvalStageResolver;
    private final SalesOrderCreationService salesOrderCreationService;
    private final NumberGenerator numberGenerator;
    private final RabbitTemplate rabbitTemplate;
    private final MessageService messageService;
    private final PlatformWalletUsageSupport platformWalletUsageSupport;
    private final ProcurementApproverSupport procurementApproverSupport;
    private final WarehouseLocationRepository warehouseLocationRepository;

    private static final String INVENTORY_EXCHANGE = "inventory.exchange";
    private static final String PO_APPROVED_ROUTING_KEY = "po.approved";
    private static final String SALES_ORDER_APPROVED_ROUTING_KEY = "sales.order.approved";

    // ============================================================
    // 1. PR INTERNAL APPROVAL
    // ============================================================

    /**
     * Approve an internal approval stage on a Purchase Requisition.
     *
     * FLOW:
     * 1. Load PR (must be SUBMITTED status)
     * 2. Determine required stages via resolver (uses org-level or platform default)
     * 3. Record approval via ProcurementApprovalService
     * 4. If all stages complete → APPROVED
     * 5. If preferredSupplierId set → auto-publish to supplier (PUBLISHED_TO_SUPPLIER)
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseRequisitionResponse approveInternalStage(ApprovePurchaseRequisitionRequest request,
                                                            Locale locale, String username) {
        log.info("Approving internal PR stage for PR id: {} by user: {}", request.getId(), username);

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository
                .findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);

        if (prOpt.isEmpty()) {
            return buildPRResponse(404, false, "Purchase requisition not found.", null);
        }

        PurchaseRequisition pr = prOpt.get();

        if (pr.getStatus() != PurchaseRequisitionStatus.SUBMITTED) {
            return buildPRResponse(400, false,
                    "Purchase requisition must be in SUBMITTED status to approve. Current: " + pr.getStatus(), null);
        }

        Optional<String> approverError = procurementApproverSupport.validateApproverForOrganization(
                request.getApprovedByUserId(), pr.getOrganizationId(), locale);
        if (approverError.isPresent()) {
            return buildPRResponse(403, false, approverError.get(), null);
        }

        // ============================================================
        // STEP 1: Determine required stages
        // ============================================================
        int requiredStages = pr.getRequiredApprovalStages() != null
                ? pr.getRequiredApprovalStages()
                : approvalStageResolver.resolveRequiredStages(pr.getOrganizationId());

        int currentStage = pr.getCurrentApprovalStage() != null ? pr.getCurrentApprovalStage() : 0;

        // ============================================================
        // STEP 2: Record approval via ProcurementApprovalService
        // ============================================================
        StageApprovalResult result = procurementApprovalService.recordApproval(
                ProcurementApprovalDocumentType.REQUISITION_INTERNAL,
                pr.getId(),
                currentStage,
                requiredStages,
                request.getApprovedByUserId(),
                username,
                request.getApprovalNotes()
        );

        pr.setCurrentApprovalStage(result.completedStage());

        // ============================================================
        // STEP 3: Transition status if all stages complete
        // ============================================================
        if (result.allStagesComplete()) {
            log.info("All {} approval stages complete for PR {} - transitioning to APPROVED", requiredStages, pr.getId());
            pr.setStatus(PurchaseRequisitionStatus.APPROVED);
            pr.setApprovedAt(LocalDateTime.now());
            pr.setApprovedByUserId(request.getApprovedByUserId());
            pr.setApprovalNotes(request.getApprovalNotes());

            // Default: approve all quantities as requested if no line adjustments
            if (request.getLineApprovals() == null || request.getLineApprovals().isEmpty()) {
                for (var line : pr.getLines()) {
                    if (line.getApprovedQuantity() == null) {
                        line.setApprovedQuantity(line.getRequestedQuantity());
                    }
                    line.calculateRemainingQuantity();
                }
            } else {
                for (var lineApproval : request.getLineApprovals()) {
                    pr.getLines().stream()
                            .filter(l -> l.getId().equals(lineApproval.getLineId()))
                            .findFirst()
                            .ifPresent(line -> {
                                line.setApprovedQuantity(lineApproval.getApprovedQuantity() != null
                                        ? lineApproval.getApprovedQuantity()
                                        : line.getRequestedQuantity());
                                if (lineApproval.getFulfillmentMethod() != null) {
                                    line.setFulfillmentMethod(lineApproval.getFulfillmentMethod());
                                }
                                if (lineApproval.getQuantityAdjustmentReason() != null) {
                                    line.setQuantityAdjustmentReason(lineApproval.getQuantityAdjustmentReason());
                                }
                                line.calculateRemainingQuantity();
                            });
                }
            }

            // ============================================================
            // STEP 4: Auto-publish to supplier if preferredSupplierId is set
            // ============================================================
            if (pr.getPreferredSupplierId() != null) {
                log.info("PR {} has preferredSupplierId {} - auto-publishing to supplier",
                        pr.getId(), pr.getPreferredSupplierId());
                pr.setStatus(PurchaseRequisitionStatus.PUBLISHED_TO_SUPPLIER);
            }
        } else {
            log.info("PR {} approval stage {}/{} complete - awaiting further approval",
                    pr.getId(), result.completedStage(), requiredStages);
        }

        PurchaseRequisition saved = purchaseRequisitionRepository.save(pr);
        platformWalletUsageSupport.chargeBestEffort(
                pr.getOrganizationId(),
                PlatformWalletUsageSupport.ACTION_PROCUREMENT_PR_APPROVE,
                "PURCHASE_REQUISITION",
                saved.getId());
        return buildPRResponse(200, true, "Purchase requisition stage approved.", mapPrToDto(saved));
    }

    // ============================================================
    // 2. PUBLISH TO SUPPLIER
    // ============================================================

    /**
     * Manually publish an APPROVED PR to the supplier.
     *
     * FLOW: APPROVED → PUBLISHED_TO_SUPPLIER
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseRequisitionResponse publishToSupplier(Long requisitionId, Locale locale, String username) {
        log.info("Publishing PR {} to supplier by user: {}", requisitionId, username);

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository
                .findByIdAndEntityStatusNot(requisitionId, EntityStatus.DELETED);

        if (prOpt.isEmpty()) {
            return buildPRResponse(404, false, "Purchase requisition not found.", null);
        }

        PurchaseRequisition pr = prOpt.get();

        if (pr.getStatus() != PurchaseRequisitionStatus.APPROVED) {
            return buildPRResponse(400, false,
                    "Purchase requisition must be APPROVED to publish to supplier. Current: " + pr.getStatus(), null);
        }

        pr.setStatus(PurchaseRequisitionStatus.PUBLISHED_TO_SUPPLIER);
        PurchaseRequisition saved = purchaseRequisitionRepository.save(pr);

        return buildPRResponse(200, true, "Purchase requisition published to supplier.", mapPrToDto(saved));
    }

    // ============================================================
    // 3. FIND SUPPLIER VISIBLE REQUISITIONS
    // ============================================================

    /**
     * Returns all PRs visible to a supplier (PUBLISHED_TO_SUPPLIER status, matching preferredSupplierId).
     */
    @Override
    @Transactional(readOnly = true)
    public PurchaseRequisitionResponse findSupplierVisibleRequisitions(Long supplierOrganizationId,
                                                                       Locale locale, String username) {
        log.info("Finding supplier-visible PRs for supplier org: {}", supplierOrganizationId);

        List<PurchaseRequisition> prs = purchaseRequisitionRepository
                .findByPreferredSupplierIdAndStatusAndEntityStatusNot(
                        supplierOrganizationId,
                        PurchaseRequisitionStatus.PUBLISHED_TO_SUPPLIER,
                        EntityStatus.DELETED);

        List<PurchaseRequisitionDto> dtos = prs.stream().map(this::mapPrToDto).collect(Collectors.toList());
        PurchaseRequisitionResponse response = new PurchaseRequisitionResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Supplier-visible requisitions retrieved.");
        response.setPurchaseRequisitionDtoList(dtos);
        return response;
    }

    // ============================================================
    // 4. SUBMIT SUPPLIER QUOTE
    // ============================================================

    /**
     * Supplier submits a quote against a published PR.
     *
     * FLOW:
     * 1. Load PR (must be PUBLISHED_TO_SUPPLIER)
     * 2. Create SupplierQuote + SupplierQuoteLines
     * 3. Set PR status to SUPPLIER_CONFIRMED
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SupplierQuoteResponse submitSupplierQuote(SubmitSupplierQuoteRequest request,
                                                     Locale locale, String username) {
        log.info("Submitting supplier quote for PR: {} by supplier org: {}",
                request.getPurchaseRequisitionId(), request.getSupplierOrganizationId());

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository
                .findByIdAndEntityStatusNot(request.getPurchaseRequisitionId(), EntityStatus.DELETED);

        if (prOpt.isEmpty()) {
            return buildQuoteResponse(404, false, "Purchase requisition not found.", null);
        }

        PurchaseRequisition pr = prOpt.get();

        if (pr.getStatus() != PurchaseRequisitionStatus.PUBLISHED_TO_SUPPLIER) {
            return buildQuoteResponse(400, false,
                    "Purchase requisition must be PUBLISHED_TO_SUPPLIER to submit a quote. Current: " + pr.getStatus(),
                    null);
        }

        List<String> validationErrors = SupplierQuoteSubmissionValidator.validate(request);
        if (!validationErrors.isEmpty()) {
            return buildQuoteResponse(400, false, "Supplier quote validation failed.", null, validationErrors);
        }

        // ============================================================
        // STEP 1: Create SupplierQuote
        // ============================================================
        SupplierQuote quote = new SupplierQuote();
        quote.setQuoteNumber(numberGenerator.generateNumber("SQ"));
        quote.setPurchaseRequisitionId(pr.getId());
        quote.setSupplierOrganizationId(request.getSupplierOrganizationId());
        quote.setCustomerOrganizationId(pr.getOrganizationId());
        quote.setStatus(SupplierQuoteStatus.SUBMITTED);
        quote.setQuoteSource(request.getQuoteSource() != null
                ? request.getQuoteSource()
                : SupplierQuoteSource.SYSTEM_GENERATED);
        quote.setExternalDocumentId(request.getExternalDocumentId());
        quote.setCurrency(request.getCurrency() != null ? request.getCurrency() : pr.getCurrency());
        quote.setTaxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : java.math.BigDecimal.ZERO);
        quote.setPaymentTerm(request.getPaymentTerm());
        quote.setDeliveryTerms(request.getDeliveryTerms());
        quote.setValidityUntil(request.getValidityUntil());
        quote.setNotes(request.getNotes());
        quote.setSubmittedAt(LocalDateTime.now());
        quote.setSubmittedByUserId(request.getSubmittedByUserId());
        quote.setEntityStatus(EntityStatus.ACTIVE);
        quote.setCreatedBy(username);

        // ============================================================
        // STEP 2: Create SupplierQuoteLines
        // ============================================================
        if (request.getLines() != null) {
            int lineNum = 1;
            for (var lineReq : request.getLines()) {
                SupplierQuoteLine line = new SupplierQuoteLine();
                line.setSupplierQuote(quote);
                line.setPurchaseRequisitionLineId(lineReq.getPurchaseRequisitionLineId());
                line.setLineNumber(lineNum++);
                line.setProductId(lineReq.getProductId());
                line.setQuotedQuantity(lineReq.getQuotedQuantity());
                line.setUnitPrice(lineReq.getUnitPrice());
                line.setLeadTimeDays(lineReq.getLeadTimeDays());
                line.setNotes(lineReq.getNotes());
                line.setCreatedBy(username);
                quote.getLines().add(line);
            }
        }

        quote.calculateTotals();
        SupplierQuote savedQuote = supplierQuoteRepository.save(quote);

        // ============================================================
        // STEP 3: Transition PR to SUPPLIER_CONFIRMED
        // ============================================================
        pr.setStatus(PurchaseRequisitionStatus.SUPPLIER_CONFIRMED);
        pr.setSupplierQuoteId(savedQuote.getId());
        purchaseRequisitionRepository.save(pr);

        log.info("Supplier quote {} created for PR {} - PR set to SUPPLIER_CONFIRMED",
                savedQuote.getQuoteNumber(), pr.getId());

        platformWalletUsageSupport.chargeBestEffort(
                savedQuote.getSupplierOrganizationId(),
                PlatformWalletUsageSupport.ACTION_PROCUREMENT_QUOTE_SUBMIT,
                "SUPPLIER_QUOTE",
                savedQuote.getId());

        return buildQuoteResponse(201, true, "Supplier quote submitted successfully.", mapQuoteToDto(savedQuote));
    }

    // ============================================================
    // 5. ACKNOWLEDGE SUPPLIER QUOTE
    // ============================================================

    /**
     * Customer acknowledges the supplier's quote, moving PR to CUSTOMER_ACKNOWLEDGED.
     *
     * FLOW: SUPPLIER_CONFIRMED → CUSTOMER_ACKNOWLEDGED
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseRequisitionResponse acknowledgeSupplierQuote(AcknowledgeSupplierQuoteRequest request,
                                                                Locale locale, String username) {
        log.info("Acknowledging supplier quote for PR: {} by user: {}", request.getPurchaseRequisitionId(), username);

        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository
                .findByIdAndEntityStatusNot(request.getPurchaseRequisitionId(), EntityStatus.DELETED);

        if (prOpt.isEmpty()) {
            return buildPRResponse(404, false, "Purchase requisition not found.", null);
        }

        PurchaseRequisition pr = prOpt.get();

        if (pr.getStatus() != PurchaseRequisitionStatus.SUPPLIER_CONFIRMED) {
            return buildPRResponse(400, false,
                    "Purchase requisition must be SUPPLIER_CONFIRMED to acknowledge. Current: " + pr.getStatus(), null);
        }

        pr.setStatus(PurchaseRequisitionStatus.CUSTOMER_ACKNOWLEDGED);
        PurchaseRequisition saved = purchaseRequisitionRepository.save(pr);

        log.info("PR {} acknowledged by customer - status: CUSTOMER_ACKNOWLEDGED", pr.getId());
        return buildPRResponse(200, true, "Supplier quote acknowledged.", mapPrToDto(saved));
    }

    // ============================================================
    // 6. APPROVE PO CUSTOMER STAGE
    // ============================================================

    /**
     * Customer approves a stage on the Purchase Order.
     *
     * FLOW (multi-stage):
     * SUBMITTED → PENDING_CUSTOMER_APPROVAL (stage 1..n-1)
     * Final stage → CUSTOMER_APPROVED → PENDING_SUPPLIER_APPROVAL
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseOrderResponse approvePurchaseOrderCustomerStage(ApprovePurchaseOrderStageRequest request,
                                                                   Locale locale, String username) {
        log.info("Approving PO customer stage for PO: {} by user: {}", request.getPurchaseOrderId(), username);

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository
                .findByIdAndEntityStatusNot(request.getPurchaseOrderId(), EntityStatus.DELETED);

        if (poOpt.isEmpty()) {
            return buildPOResponse(404, false, "Purchase order not found.", null);
        }

        PurchaseOrder po = poOpt.get();

        // Accept SUBMITTED or PENDING_CUSTOMER_APPROVAL
        if (po.getStatus() != PurchaseOrderStatus.SUBMITTED
                && po.getStatus() != PurchaseOrderStatus.PENDING_CUSTOMER_APPROVAL) {
            return buildPOResponse(400, false,
                    "Purchase order must be SUBMITTED or PENDING_CUSTOMER_APPROVAL for customer stage approval. Current: " + po.getStatus(),
                    null);
        }

        Optional<String> customerApproverError = procurementApproverSupport.validateApproverForOrganization(
                request.getApprovedByUserId(), po.getOrganizationId(), locale);
        if (customerApproverError.isPresent()) {
            return buildPOResponse(403, false, customerApproverError.get(), null);
        }

        // ============================================================
        // STEP 1: Determine required stages
        // ============================================================
        int requiredStages = po.getRequiredApprovalStages() != null
                ? po.getRequiredApprovalStages()
                : approvalStageResolver.resolveRequiredStages(po.getOrganizationId());

        int currentStage = po.getCurrentCustomerApprovalStage() != null
                ? po.getCurrentCustomerApprovalStage()
                : 0;

        // ============================================================
        // STEP 2: Record approval
        // ============================================================
        StageApprovalResult result = procurementApprovalService.recordApproval(
                ProcurementApprovalDocumentType.PO_CUSTOMER,
                po.getId(),
                currentStage,
                requiredStages,
                request.getApprovedByUserId(),
                username,
                request.getApprovalNotes()
        );

        po.setCurrentCustomerApprovalStage(result.completedStage());

        // ============================================================
        // STEP 3: Transition status
        // ============================================================
        if (result.allStagesComplete()) {
            log.info("All {} customer approval stages complete for PO {} - moving to CUSTOMER_APPROVED → PENDING_SUPPLIER_APPROVAL",
                    requiredStages, po.getId());
            po.setStatus(PurchaseOrderStatus.CUSTOMER_APPROVED);
            po.setCustomerApprovalComplete(true);
            po.setCustomerApprovedAt(LocalDateTime.now());
            po.setApprovalNotes(request.getApprovalNotes());
            // Immediately queue for supplier approval
            po.setStatus(PurchaseOrderStatus.PENDING_SUPPLIER_APPROVAL);
        } else {
            po.setStatus(PurchaseOrderStatus.PENDING_CUSTOMER_APPROVAL);
            log.info("PO {} customer approval stage {}/{} complete - awaiting further approval",
                    po.getId(), result.completedStage(), requiredStages);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        platformWalletUsageSupport.chargeBestEffort(
                po.getOrganizationId(),
                PlatformWalletUsageSupport.ACTION_PROCUREMENT_PO_CUSTOMER_APPROVE,
                "PURCHASE_ORDER",
                saved.getId());
        return buildPOResponse(200, true, "Purchase order customer stage approved.", mapPoToDto(saved));
    }

    // ============================================================
    // 7. APPROVE PO SUPPLIER STAGE
    // ============================================================

    /**
     * Supplier approves their stage on the Purchase Order.
     *
     * FLOW (multi-stage):
     * PENDING_SUPPLIER_APPROVAL → (stages) → APPROVED
     * On final stage: publishes RabbitMQ po.approved event
     *
     * NOTE: SO creation is handled downstream by a separate billing payment listener,
     * NOT here in the event handler.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseOrderResponse approvePurchaseOrderSupplierStage(ApprovePurchaseOrderStageRequest request,
                                                                   Locale locale, String username) {
        log.info("Approving PO supplier stage for PO: {} by user: {}", request.getPurchaseOrderId(), username);

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository
                .findByIdAndEntityStatusNot(request.getPurchaseOrderId(), EntityStatus.DELETED);

        if (poOpt.isEmpty()) {
            return buildPOResponse(404, false, "Purchase order not found.", null);
        }

        PurchaseOrder po = poOpt.get();

        if (po.getStatus() != PurchaseOrderStatus.PENDING_SUPPLIER_APPROVAL) {
            return buildPOResponse(400, false,
                    "Purchase order must be in PENDING_SUPPLIER_APPROVAL status. Current: " + po.getStatus(), null);
        }

        Optional<String> supplierApproverError = procurementApproverSupport.validateApproverForOrganization(
                request.getApprovedByUserId(), po.getSupplierId(), locale);
        if (supplierApproverError.isPresent()) {
            return buildPOResponse(403, false, supplierApproverError.get(), null);
        }

        // ============================================================
        // STEP 1: Determine required stages
        // ============================================================
        int requiredStages = po.getRequiredApprovalStages() != null
                ? po.getRequiredApprovalStages()
                : approvalStageResolver.resolveRequiredStages(po.getOrganizationId());

        int currentStage = po.getCurrentSupplierApprovalStage() != null
                ? po.getCurrentSupplierApprovalStage()
                : 0;

        // ============================================================
        // STEP 2: Record approval
        // ============================================================
        StageApprovalResult result = procurementApprovalService.recordApproval(
                ProcurementApprovalDocumentType.PO_SUPPLIER,
                po.getId(),
                currentStage,
                requiredStages,
                request.getApprovedByUserId(),
                username,
                request.getApprovalNotes()
        );

        po.setCurrentSupplierApprovalStage(result.completedStage());

        // ============================================================
        // STEP 3: Transition to APPROVED on final stage
        // ============================================================
        if (result.allStagesComplete()) {
            log.info("All {} supplier approval stages complete for PO {} - transitioning to APPROVED",
                    requiredStages, po.getId());
            po.setStatus(PurchaseOrderStatus.APPROVED);
            po.setSupplierApprovalComplete(true);
            po.setSupplierApprovedAt(LocalDateTime.now());
            po.setApprovedByUserId(request.getApprovedByUserId());
            po.setApprovedAt(LocalDateTime.now());
            po.setApprovalNotes(request.getApprovalNotes());
            PurchaseOrder saved = purchaseOrderRepository.save(po);

            // ============================================================
            // STEP 4: Publish po.approved RabbitMQ event
            // ============================================================
            try {
                Map<String, Object> event = new HashMap<>();
                event.put("purchaseOrderId", po.getId());
                event.put("purchaseOrderNumber", po.getPurchaseOrderNumber());
                event.put("supplierOrganizationId", po.getSupplierId());
                event.put("organizationId", po.getOrganizationId());
                event.put("totalAmount", po.getTotalAmount());
                event.put("approvedByUserId", request.getApprovedByUserId());
                event.put("approvedAt", LocalDateTime.now().toString());

                rabbitTemplate.convertAndSend(INVENTORY_EXCHANGE, PO_APPROVED_ROUTING_KEY, event);
                log.info("Published po.approved event for PO: {}", po.getId());
            } catch (Exception ex) {
                log.error("Failed to publish po.approved event for PO {}: {}", po.getId(), ex.getMessage(), ex);
            }

            platformWalletUsageSupport.chargeBestEffort(
                    po.getSupplierId(),
                    PlatformWalletUsageSupport.ACTION_PROCUREMENT_PO_SUPPLIER_APPROVE,
                    "PURCHASE_ORDER",
                    saved.getId());

            return buildPOResponse(200, true, "Purchase order supplier stage approved. PO is now APPROVED.", mapPoToDto(saved));
        } else {
            log.info("PO {} supplier approval stage {}/{} complete - awaiting further approval",
                    po.getId(), result.completedStage(), requiredStages);
            PurchaseOrder saved = purchaseOrderRepository.save(po);
            platformWalletUsageSupport.chargeBestEffort(
                    po.getSupplierId(),
                    PlatformWalletUsageSupport.ACTION_PROCUREMENT_PO_SUPPLIER_APPROVE,
                    "PURCHASE_ORDER",
                    saved.getId());
            return buildPOResponse(200, true,
                    "Purchase order supplier stage " + result.completedStage() + "/" + requiredStages + " approved.",
                    mapPoToDto(saved));
        }
    }

    // ============================================================
    // 8. APPROVE SALES ORDER STAGE
    // ============================================================

    /**
     * Approves a stage on a Sales Order.
     *
     * FLOW: PENDING_APPROVAL → (multi-stage) → APPROVED
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SalesOrderResponse approveSalesOrderStage(ApproveSalesOrderStageRequest request,
                                                     Locale locale, String username) {
        log.info("Approving SO stage for SO: {} by user: {}", request.getSalesOrderId(), username);

        Optional<SalesOrder> soOpt = salesOrderRepository
                .findByIdAndEntityStatusNot(request.getSalesOrderId(), EntityStatus.DELETED);

        if (soOpt.isEmpty()) {
            return buildSOResponse(404, false, "Sales order not found.", null);
        }

        SalesOrder so = soOpt.get();

        if (so.getStatus() != SalesOrderStatus.PENDING_APPROVAL) {
            return buildSOResponse(400, false,
                    "Sales order must be in PENDING_APPROVAL status for stage approval. Current: " + so.getStatus(),
                    null);
        }

        Optional<String> soApproverError = procurementApproverSupport.validateApproverForOrganization(
                request.getApprovedByUserId(), so.getSupplierOrganizationId(), locale);
        if (soApproverError.isPresent()) {
            return buildSOResponse(403, false, soApproverError.get(), null);
        }

        // ============================================================
        // STEP 1: Determine required stages
        // ============================================================
        int requiredStages = so.getRequiredApprovalStages() != null
                ? so.getRequiredApprovalStages()
                : approvalStageResolver.resolveRequiredStages(so.getSupplierOrganizationId());

        int currentStage = so.getCurrentApprovalStage() != null ? so.getCurrentApprovalStage() : 0;

        // ============================================================
        // STEP 2: Record approval
        // ============================================================
        StageApprovalResult result = procurementApprovalService.recordApproval(
                ProcurementApprovalDocumentType.SALES_ORDER,
                so.getId(),
                currentStage,
                requiredStages,
                request.getApprovedByUserId(),
                username,
                request.getApprovalNotes()
        );

        so.setCurrentApprovalStage(result.completedStage());

        // ============================================================
        // STEP 3: Transition to APPROVED on final stage
        // ============================================================
        if (result.allStagesComplete()) {
            log.info("All {} SO approval stages complete for SO {} - transitioning to APPROVED",
                    requiredStages, so.getId());

            if (request.getFulfillmentWarehouseId() != null) {
                so.setFulfillmentWarehouseId(request.getFulfillmentWarehouseId());
            }
            if (so.getFulfillmentWarehouseId() == null) {
                return buildSOResponse(400, false,
                        "Fulfillment warehouse is required before sales order approval completes.", null);
            }

            so.setStatus(SalesOrderStatus.APPROVED);
            so.setApprovalComplete(true);
            so.setApprovedAt(LocalDateTime.now());
            so.setApprovedByUserId(request.getApprovedByUserId());
        } else {
            log.info("SO {} approval stage {}/{} complete - awaiting further approval",
                    so.getId(), result.completedStage(), requiredStages);
        }

        SalesOrder saved = salesOrderRepository.save(so);
        if (result.allStagesComplete()) {
            publishSalesOrderApprovedEvent(saved, request.getApprovedByUserId());
        }
        platformWalletUsageSupport.chargeBestEffort(
                so.getSupplierOrganizationId(),
                PlatformWalletUsageSupport.ACTION_PROCUREMENT_SO_APPROVE,
                "SALES_ORDER",
                saved.getId());
        return buildSOResponse(200, true, "Sales order stage approved.", mapSoToDto(saved));
    }

    // ============================================================
    // 9. CREATE SALES ORDER FROM PAID PURCHASE ORDER
    // ============================================================

    /**
     * Creates a Sales Order from a PO that has been paid/confirmed by billing.
     *
     * The SO is created in PENDING_APPROVAL status (not AWAITING_RECEIPT),
     * because payment has been verified and goods can now be committed.
     *
     * This is called by the PaymentVerifiedEventHandler after billing confirms payment.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SalesOrderResponse createSalesOrderFromPaidPurchaseOrder(Long purchaseOrderId,
                                                                    Long supplierOrganizationId,
                                                                    Long createdByUserId,
                                                                    Locale locale, String username) {
        log.info("Creating SO from paid PO: {} for supplier org: {}", purchaseOrderId, supplierOrganizationId);

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository
                .findByIdAndEntityStatusNot(purchaseOrderId, EntityStatus.DELETED);

        if (poOpt.isEmpty()) {
            return buildSOResponse(404, false, "Purchase order not found.", null);
        }

        PurchaseOrder po = poOpt.get();

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            return buildSOResponse(400, false,
                    "Purchase order must be APPROVED to create a Sales Order. Current: " + po.getStatus(), null);
        }

        // Check if SO already exists
        if (salesOrderRepository.findByPurchaseOrderId(purchaseOrderId).isPresent()) {
            return buildSOResponse(409, false,
                    "Sales Order already exists for Purchase Order: " + purchaseOrderId, null);
        }

        // ============================================================
        // STEP 1: Resolve supplier org ID from PO if not provided
        // ============================================================
        Long resolvedSupplierOrgId = supplierOrganizationId != null
                ? supplierOrganizationId
                : po.getSupplierId();

        // ============================================================
        // STEP 2: Create SO via creation service
        // ============================================================
        SalesOrder salesOrder = salesOrderCreationService.createFromPurchaseOrder(
                po, resolvedSupplierOrgId, createdByUserId, locale);

        // ============================================================
        // STEP 3: Override status to PENDING_APPROVAL
        // ============================================================
        salesOrder.setStatus(SalesOrderStatus.PENDING_APPROVAL);
        salesOrder.setRequiredApprovalStages(
                approvalStageResolver.resolveRequiredStages(resolvedSupplierOrgId));

        SalesOrder saved = salesOrderRepository.save(salesOrder);

        log.info("Created SO {} (PENDING_APPROVAL) from paid PO {}", saved.getSalesOrderNumber(), po.getPurchaseOrderNumber());
        return buildSOResponse(201, true, "Sales order created from paid purchase order.", mapSoToDto(saved));
    }

    // ============================================================
    // 10. FIND SUPPLIER QUOTES
    // ============================================================

    /**
     * Returns all quotes submitted by a supplier organisation, newest first.
     * Each quote is enriched with the linked requisition number where available.
     */
    @Override
    @Transactional(readOnly = true)
    public SupplierQuoteResponse findSupplierQuotes(Long supplierOrganizationId, Locale locale, String username) {
        log.info("Finding quotes for supplier org: {} requested by user: {}", supplierOrganizationId, username);

        List<SupplierQuote> quotes = supplierQuoteRepository
                .findBySupplierOrganizationIdAndEntityStatusNotOrderBySubmittedAtDesc(
                        supplierOrganizationId, EntityStatus.DELETED);

        List<SupplierQuoteDto> dtos = enrichQuotesWithRequisitionNumbers(quotes);

        SupplierQuoteResponse response = new SupplierQuoteResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Supplier quotes retrieved.");
        response.setSupplierQuoteDtoList(dtos);
        return response;
    }

    // ============================================================
    // 11. FIND CUSTOMER QUOTES
    // ============================================================

    /**
     * Returns all quotes received by a customer organisation, newest first.
     * Each quote is enriched with the linked requisition number where available.
     */
    @Override
    @Transactional(readOnly = true)
    public SupplierQuoteResponse findCustomerQuotes(Long customerOrganizationId, Locale locale, String username) {
        log.info("Finding quotes for customer org: {} requested by user: {}", customerOrganizationId, username);

        List<SupplierQuote> quotes = supplierQuoteRepository
                .findByCustomerOrganizationIdAndEntityStatusNotOrderBySubmittedAtDesc(
                        customerOrganizationId, EntityStatus.DELETED);

        List<SupplierQuoteDto> dtos = enrichQuotesWithRequisitionNumbers(quotes);

        SupplierQuoteResponse response = new SupplierQuoteResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Customer quotes retrieved.");
        response.setSupplierQuoteDtoList(dtos);
        return response;
    }

    // ============================================================
    // 12. FIND CUSTOMER SALES ORDERS
    // ============================================================

    /**
     * Returns all sales orders for a customer organisation, newest first.
     * Each SO is linked to the originating purchase order.
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponse findCustomerSalesOrders(Long customerOrganizationId, Locale locale, String username) {
        log.info("Finding sales orders for customer org: {} requested by user: {}", customerOrganizationId, username);

        List<SalesOrder> orders = salesOrderRepository
                .findByCustomerIdAndEntityStatusNotOrderByCreatedAtDesc(customerOrganizationId, EntityStatus.DELETED);

        List<SalesOrderDto> dtos = orders.stream().map(this::mapSoToDto).toList();

        SalesOrderResponse response = new SalesOrderResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Customer sales orders retrieved.");
        response.setSalesOrderDtoList(dtos);
        return response;
    }

    // ============================================================
    // 13. FIND QUOTE BY REQUISITION ID
    // ============================================================

    /**
     * Returns the latest supplier quote for a given requisition, including all lines.
     *
     * FLOW:
     * 1. Load the PR to verify it exists and resolve requisitionNumber
     * 2. Find all quotes for that PR ordered by createdAt desc → take the first
     * 3. Map to DTO with lines
     */
    @Override
    @Transactional(readOnly = true)
    public SupplierQuoteResponse findQuoteByRequisitionId(Long requisitionId, Locale locale, String username) {
        log.info("Finding latest quote for requisition: {} by user: {}", requisitionId, username);

        // ============================================================
        // STEP 1: Verify the requisition exists
        // ============================================================
        Optional<PurchaseRequisition> prOpt = purchaseRequisitionRepository
                .findByIdAndEntityStatusNot(requisitionId, EntityStatus.DELETED);

        if (prOpt.isEmpty()) {
            return buildQuoteResponse(404, false, "Purchase requisition not found.", null);
        }

        PurchaseRequisition pr = prOpt.get();

        // ============================================================
        // STEP 2: Load quotes for this requisition (newest first)
        // ============================================================
        List<SupplierQuote> quotes = supplierQuoteRepository
                .findByPurchaseRequisitionIdAndEntityStatusNotOrderByCreatedAtDesc(
                        requisitionId, EntityStatus.DELETED);

        if (quotes.isEmpty()) {
            return buildQuoteResponse(404, false,
                    "No supplier quote found for requisition: " + pr.getRequisitionNumber(), null);
        }

        // ============================================================
        // STEP 3: Map latest quote with lines and requisition number
        // ============================================================
        SupplierQuote latest = quotes.get(0);
        SupplierQuoteDto dto = mapQuoteToDtoWithRequisitionNumber(latest, pr.getRequisitionNumber());

        return buildQuoteResponse(200, true, "Supplier quote retrieved.", dto);
    }

    /**
     * Enriches a list of quotes with their linked PR's requisitionNumber.
     * Performs a batch lookup keyed on purchaseRequisitionId to avoid N+1 queries.
     */
    private List<SupplierQuoteDto> enrichQuotesWithRequisitionNumbers(List<SupplierQuote> quotes) {
        if (quotes.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect distinct PR IDs and load them in one go
        Set<Long> prIds = quotes.stream()
                .map(SupplierQuote::getPurchaseRequisitionId)
                .collect(Collectors.toSet());

        Map<Long, String> prIdToNumber = purchaseRequisitionRepository.findAllById(prIds)
                .stream()
                .filter(pr -> pr.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toMap(
                        PurchaseRequisition::getId,
                        PurchaseRequisition::getRequisitionNumber));

        return quotes.stream()
                .map(q -> mapQuoteToDtoWithRequisitionNumber(
                        q, prIdToNumber.get(q.getPurchaseRequisitionId())))
                .collect(Collectors.toList());
    }

    // ============================================================
    // MAPPING HELPERS
    // ============================================================

    private PurchaseRequisitionDto mapPrToDto(PurchaseRequisition pr) {
        PurchaseRequisitionDto dto = new PurchaseRequisitionDto();
        dto.setId(pr.getId());
        dto.setRequisitionNumber(pr.getRequisitionNumber());
        dto.setOrganizationId(pr.getOrganizationId());
        dto.setDepartmentId(pr.getDepartmentId());
        dto.setRequestedByUserId(pr.getRequestedByUserId());
        dto.setPurpose(pr.getPurpose());
        dto.setJustification(pr.getJustification());
        dto.setPriority(pr.getPriority());
        dto.setRequisitionDate(pr.getRequisitionDate());
        dto.setRequiredByDate(pr.getRequiredByDate());
        dto.setStatus(pr.getStatus());
        dto.setPreferredSupplierId(pr.getPreferredSupplierId());
        dto.setCurrentApprovalStage(pr.getCurrentApprovalStage());
        dto.setRequiredApprovalStages(pr.getRequiredApprovalStages());
        dto.setSupplierQuoteId(pr.getSupplierQuoteId());
        dto.setEstimatedTotal(pr.getEstimatedTotal());
        dto.setCreatedAt(pr.getCreatedAt());
        return dto;
    }

    private PurchaseOrderDto mapPoToDto(PurchaseOrder po) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(po.getId());
        dto.setPurchaseOrderNumber(po.getPurchaseOrderNumber());
        dto.setOrganizationId(po.getOrganizationId());
        dto.setSupplierId(po.getSupplierId());
        dto.setStatus(po.getStatus());
        dto.setCurrentCustomerApprovalStage(po.getCurrentCustomerApprovalStage());
        dto.setCurrentSupplierApprovalStage(po.getCurrentSupplierApprovalStage());
        dto.setRequiredApprovalStages(po.getRequiredApprovalStages());
        dto.setCustomerApprovalComplete(po.getCustomerApprovalComplete());
        dto.setSupplierApprovalComplete(po.getSupplierApprovalComplete());
        dto.setTotalAmount(po.getTotalAmount());
        return dto;
    }

    private SalesOrderDto mapSoToDto(SalesOrder so) {
        SalesOrderDto dto = new SalesOrderDto();
        dto.setId(so.getId());
        dto.setSalesOrderNumber(so.getSalesOrderNumber());
        dto.setPurchaseOrderId(so.getPurchaseOrderId());
        dto.setPurchaseOrderNumber(so.getPurchaseOrderNumber());
        dto.setSupplierOrganizationId(so.getSupplierOrganizationId());
        dto.setCustomerId(so.getCustomerId());
        dto.setStatus(so.getStatus());
        dto.setCurrentApprovalStage(so.getCurrentApprovalStage());
        dto.setRequiredApprovalStages(so.getRequiredApprovalStages());
        dto.setTotalAmount(so.getTotalAmount());
        dto.setOrderDate(so.getOrderDate());
        dto.setExpectedDeliveryDate(so.getExpectedDeliveryDate());
        dto.setCreatedAt(so.getCreatedAt());
        return dto;
    }

    private SupplierQuoteDto mapQuoteToDto(SupplierQuote quote) {
        return mapQuoteToDtoWithRequisitionNumber(quote, null);
    }

    private SupplierQuoteDto mapQuoteToDtoWithRequisitionNumber(SupplierQuote quote, String requisitionNumber) {
        SupplierQuoteDto dto = new SupplierQuoteDto();
        dto.setId(quote.getId());
        dto.setQuoteNumber(quote.getQuoteNumber());
        dto.setPurchaseRequisitionId(quote.getPurchaseRequisitionId());
        dto.setRequisitionNumber(requisitionNumber);
        dto.setSupplierOrganizationId(quote.getSupplierOrganizationId());
        dto.setCustomerOrganizationId(quote.getCustomerOrganizationId());
        dto.setStatus(quote.getStatus());
        dto.setQuoteSource(quote.getQuoteSource());
        dto.setExternalDocumentId(quote.getExternalDocumentId());
        dto.setCurrency(quote.getCurrency());
        dto.setSubtotal(quote.getSubtotal());
        dto.setTaxAmount(quote.getTaxAmount());
        dto.setTotalAmount(quote.getTotalAmount());
        dto.setPaymentTerm(quote.getPaymentTerm());
        dto.setDeliveryTerms(quote.getDeliveryTerms());
        dto.setValidityUntil(quote.getValidityUntil());
        dto.setNotes(quote.getNotes());
        dto.setSubmittedAt(quote.getSubmittedAt());
        dto.setSubmittedByUserId(quote.getSubmittedByUserId());

        if (quote.getLines() != null) {
            List<SupplierQuoteLineDto> lineDtos = quote.getLines().stream().map(l -> {
                SupplierQuoteLineDto lineDto = new SupplierQuoteLineDto();
                lineDto.setId(l.getId());
                lineDto.setPurchaseRequisitionLineId(l.getPurchaseRequisitionLineId());
                lineDto.setLineNumber(l.getLineNumber());
                lineDto.setProductId(l.getProductId());
                lineDto.setQuotedQuantity(l.getQuotedQuantity());
                lineDto.setUnitPrice(l.getUnitPrice());
                lineDto.setLineTotal(l.getLineTotal());
                lineDto.setLeadTimeDays(l.getLeadTimeDays());
                lineDto.setNotes(l.getNotes());
                return lineDto;
            }).collect(Collectors.toList());
            dto.setLines(lineDtos);
        }

        return dto;
    }

    // ============================================================
    // RESPONSE BUILDERS
    // ============================================================

    private PurchaseRequisitionResponse buildPRResponse(int status, boolean success,
                                                         String message, PurchaseRequisitionDto dto) {
        PurchaseRequisitionResponse response = new PurchaseRequisitionResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setPurchaseRequisitionDto(dto);
        return response;
    }

    private PurchaseOrderResponse buildPOResponse(int status, boolean success,
                                                   String message, PurchaseOrderDto dto) {
        PurchaseOrderResponse response = new PurchaseOrderResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        if (dto != null) {
            response.setPurchaseOrderDto(dto);
        }
        return response;
    }

    private SalesOrderResponse buildSOResponse(int status, boolean success,
                                                String message, SalesOrderDto dto) {
        SalesOrderResponse response = new SalesOrderResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        if (dto != null) {
            response.setSalesOrderDto(dto);
        }
        return response;
    }

    private SupplierQuoteResponse buildQuoteResponse(int status, boolean success,
                                                      String message, SupplierQuoteDto dto) {
        return buildQuoteResponse(status, success, message, dto, null);
    }

    private SupplierQuoteResponse buildQuoteResponse(int status, boolean success,
                                                      String message, SupplierQuoteDto dto,
                                                      List<String> errorMessages) {
        SupplierQuoteResponse response = new SupplierQuoteResponse();
        response.setStatusCode(status);
        response.setSuccess(success);
        response.setMessage(message);
        response.setSupplierQuoteDto(dto);
        if (errorMessages != null && !errorMessages.isEmpty()) {
            response.setErrorMessages(errorMessages);
        }
        return response;
    }

    private void publishSalesOrderApprovedEvent(SalesOrder salesOrder, Long approvedByUserId) {
        try {
            Optional<PurchaseOrder> poOpt = purchaseOrderRepository
                    .findByIdAndEntityStatusNot(salesOrder.getPurchaseOrderId(), EntityStatus.DELETED);
            if (poOpt.isEmpty()) {
                log.warn("Cannot publish sales.order.approved — PO {} not found for SO {}",
                        salesOrder.getPurchaseOrderId(), salesOrder.getId());
                return;
            }
            PurchaseOrder purchaseOrder = poOpt.get();

            WarehouseLocation fromWarehouse = warehouseLocationRepository
                    .findById(salesOrder.getFulfillmentWarehouseId()).orElse(null);
            WarehouseLocation toWarehouse = warehouseLocationRepository
                    .findById(purchaseOrder.getReceivingWarehouseId()).orElse(null);

            Map<String, Object> event = new HashMap<>();
            event.put("salesOrderId", salesOrder.getId());
            event.put("salesOrderNumber", salesOrder.getSalesOrderNumber());
            event.put("purchaseOrderId", purchaseOrder.getId());
            event.put("purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber());
            event.put("organizationId", salesOrder.getSupplierOrganizationId());
            event.put("customerOrganizationId", salesOrder.getCustomerId());
            event.put("approvedByUserId", approvedByUserId);

            if (fromWarehouse != null) {
                event.put("fromWarehouseLocationId", fromWarehouse.getId());
                event.put("fromWarehouseName", fromWarehouse.getName());
            }
            if (toWarehouse != null) {
                event.put("toWarehouseLocationId", toWarehouse.getId());
                event.put("toWarehouseName", toWarehouse.getName());
            }

            BigDecimal totalQty = BigDecimal.ZERO;
            if (salesOrder.getSalesOrderLines() != null && !salesOrder.getSalesOrderLines().isEmpty()) {
                SalesOrderLine firstLine = salesOrder.getSalesOrderLines().get(0);
                if (firstLine.getProduct() != null) {
                    event.put("productId", firstLine.getProduct().getId());
                    event.put("productName", firstLine.getProduct().getName());
                    event.put("productCode", firstLine.getProduct().getProductCode());
                }
                for (SalesOrderLine line : salesOrder.getSalesOrderLines()) {
                    if (line.getQuantity() != null) {
                        totalQty = totalQty.add(line.getQuantity());
                    }
                }
            }
            event.put("quantity", totalQty);

            boolean crossBorder = fromWarehouse != null && toWarehouse != null
                    && fromWarehouse.getLocationId() != null && toWarehouse.getLocationId() != null
                    && !fromWarehouse.getLocationId().equals(toWarehouse.getLocationId());
            event.put("crossBorder", crossBorder);
            event.put("timestamp", LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(INVENTORY_EXCHANGE, SALES_ORDER_APPROVED_ROUTING_KEY, event);
            log.info("Published sales.order.approved event for SO: {}", salesOrder.getId());
        } catch (Exception ex) {
            log.error("Failed to publish sales.order.approved event for SO {}: {}",
                    salesOrder.getId(), ex.getMessage(), ex);
        }
    }
}
