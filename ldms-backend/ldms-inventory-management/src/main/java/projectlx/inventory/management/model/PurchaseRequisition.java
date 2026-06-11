package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Requisition (PR) - Internal request to procure goods/services.
 *
 * Key concepts:
 * - PR does NOT financially commit the organization
 * - MUST be approved before procurement action
 * - Can be fulfilled via multiple methods (PO, stock, transfer)
 * - Maintains full audit trail of all changes
 * - Supports versioning via amendments
 */
@Entity
@Table(name = "purchase_requisition", indexes = {
        @Index(name = "ux_pr_requisition_number", columnList = "requisition_number", unique = true),
        @Index(name = "idx_pr_department_status", columnList = "department_id, status"),
        @Index(name = "idx_pr_requester_status", columnList = "requested_by_user_id, status"),
        @Index(name = "idx_pr_required_date", columnList = "required_by_date"),
        @Index(name = "idx_pr_created_at", columnList = "created_at")
})
@Getter
@Setter
@ToString
public class PurchaseRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requisition_number", nullable = false, unique = true, length = 50)
    private String requisitionNumber;

    @Column(name = "external_reference", length = 100)
    private String externalReference; // External system reference if applicable

    @Column(name = "version", nullable = false)
    private Integer version = 1; // For amendment tracking

    // === REQUESTER INFORMATION ===
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Column(name = "cost_center", length = 50)
    private String costCenter; // For budget tracking

    @Column(name = "project_code", length = 50)
    private String projectCode; // If PR is for a specific project

    // === PURPOSE & JUSTIFICATION ===
    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose; // Why is this requisition needed?

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification; // Business justification for approval

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private PriorityLevel priority;

    // === DATES ===
    @Column(name = "requisition_date", nullable = false)
    private LocalDate requisitionDate;

    @Column(name = "required_by_date")
    private LocalDate requiredByDate; // When items are needed

    @Column(name = "expiry_date")
    private LocalDate expiryDate; // After this date, PR expires

    // === STATUS & WORKFLOW ===
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseRequisitionStatus status;

    @Column(name = "current_approval_stage", nullable = false)
    private Integer currentApprovalStage = 0;

    @Column(name = "required_approval_stages")
    private Integer requiredApprovalStages;

    // Submission tracking
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    // Approval tracking
    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "published_to_supplier_at")
    private LocalDateTime publishedToSupplierAt;

    @Column(name = "supplier_quote_id")
    private Long supplierQuoteId;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by_user_id")
    private Long acknowledgedByUserId;

    // Rejection tracking
    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Cancellation tracking
    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // === FULFILLMENT TRACKING ===
    @Enumerated(EnumType.STRING)
    @Column(name = "default_fulfillment_method", length = 30)
    private FulfillmentMethod defaultFulfillmentMethod; // Default for all lines

    @Column(name = "target_warehouse_id")
    private Long targetWarehouseId; // Where items should be delivered

    @Column(name = "preferred_supplier_id")
    private Long preferredSupplierId; // Suggested supplier

    // === FINANCIAL ESTIMATES (for approval) ===
    @Column(name = "estimated_total", precision = 19, scale = 4)
    private BigDecimal estimatedTotal = BigDecimal.ZERO;

    @Column(name = "currency", length = 3)
    private String currency; // ISO 4217 code

    @Column(name = "budget_available")
    private Boolean budgetAvailable; // Is budget allocated?

    @Column(name = "budget_code", length = 50)
    private String budgetCode;

    // === AUDIT FIELDS ===
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 20)
    private EntityStatus entityStatus;

    // === NOTES ===
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === RELATIONSHIPS ===
    @OneToMany(mappedBy = "purchaseRequisition", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PurchaseRequisitionLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseRequisition", cascade = CascadeType.ALL, orphanRemoval = false)
    @ToString.Exclude
    private List<PurchaseRequisitionAmendment> amendments = new ArrayList<>();

    // === LIFECYCLE METHODS ===
    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;

        // Set defaults
        if (status == null) status = PurchaseRequisitionStatus.DRAFT;
        if (requisitionDate == null) requisitionDate = LocalDate.now();
        if (priority == null) priority = PriorityLevel.NORMAL;
        if (version == null) version = 1;
        if (estimatedTotal == null) estimatedTotal = BigDecimal.ZERO;
        if (budgetAvailable == null) budgetAvailable = false;
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }

    // === BUSINESS METHODS ===

    /**
     * Calculate estimated total from all lines
     */
    public void calculateEstimatedTotal() {
        if (lines == null || lines.isEmpty()) {
            this.estimatedTotal = BigDecimal.ZERO;
            return;
        }

        this.estimatedTotal = lines.stream()
                .map(line -> {
                    if (line.getEstimatedUnitPrice() != null && line.getRequestedQuantity() != null) {
                        return line.getEstimatedUnitPrice().multiply(line.getRequestedQuantity());
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if PR can be submitted
     */
    public boolean canBeSubmitted() {
        return status == PurchaseRequisitionStatus.DRAFT &&
               lines != null && !lines.isEmpty();
    }

    /**
     * Check if PR can be approved
     */
    public boolean canBeApproved() {
        return status == PurchaseRequisitionStatus.SUBMITTED;
    }

    public boolean canBePublishedToSupplier() {
        return status == PurchaseRequisitionStatus.APPROVED && preferredSupplierId != null;
    }

    public boolean canReceiveSupplierQuote() {
        return status == PurchaseRequisitionStatus.PUBLISHED_TO_SUPPLIER;
    }

    public boolean canBeAcknowledgedByCustomer() {
        return status == PurchaseRequisitionStatus.SUPPLIER_CONFIRMED;
    }

    public boolean canRaisePurchaseOrder() {
        return status == PurchaseRequisitionStatus.CUSTOMER_ACKNOWLEDGED
                || status == PurchaseRequisitionStatus.PARTIALLY_FULFILLED;
    }

    /**
     * Check if PR can be rejected
     */
    public boolean canBeRejected() {
        return status == PurchaseRequisitionStatus.SUBMITTED;
    }

    /**
     * Check if PR can be cancelled
     */
    public boolean canBeCancelled() {
        return status != PurchaseRequisitionStatus.FULFILLED &&
               status != PurchaseRequisitionStatus.CLOSED &&
               status != PurchaseRequisitionStatus.CANCELLED;
    }

    /**
     * Check if PR is fully fulfilled
     */
    public boolean isFullyFulfilled() {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        return lines.stream()
                .allMatch(line -> {
                    BigDecimal approvedQty = line.getApprovedQuantity() != null ?
                        line.getApprovedQuantity() : BigDecimal.ZERO;
                    BigDecimal totalFulfilled = line.getTotalFulfilledQuantity();
                    return totalFulfilled.compareTo(approvedQty) >= 0;
                });
    }

    /**
     * Check if PR is partially fulfilled
     */
    public boolean isPartiallyFulfilled() {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        boolean anyFulfilled = lines.stream()
                .anyMatch(line -> line.getTotalFulfilledQuantity().compareTo(BigDecimal.ZERO) > 0);
        return anyFulfilled && !isFullyFulfilled();
    }

    /**
     * Check if PR can be edited (only in DRAFT)
     */
    public boolean canBeEdited() {
        return status == PurchaseRequisitionStatus.DRAFT;
    }

    /**
     * Check if PR requires amendment (approved PRs can't be edited directly)
     */
    public boolean requiresAmendment() {
        return status == PurchaseRequisitionStatus.APPROVED ||
               status == PurchaseRequisitionStatus.PARTIALLY_FULFILLED;
    }

    /**
     * Check if PR has expired
     */
    public boolean hasExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }
}
