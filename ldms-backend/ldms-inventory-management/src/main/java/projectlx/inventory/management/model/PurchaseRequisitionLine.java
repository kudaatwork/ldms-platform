package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Purchase Requisition Line - Individual item requested in a PR.
 *
 * Key tracking fields:
 * - requested_quantity: Original request
 * - approved_quantity: What was approved (may differ from requested)
 * - ordered_quantity: How much has been ordered via PO
 * - fulfilled_from_stock_quantity: Fulfilled from existing stock
 * - fulfilled_from_transfer_quantity: Fulfilled via internal transfer
 * - remaining_quantity: Still needs fulfillment
 *
 * This granular tracking enables:
 * - Partial fulfillment from multiple sources
 * - Mixed fulfillment methods
 * - Accurate audit trail
 */
@Entity
@Table(name = "purchase_requisition_line", indexes = {
        @Index(name = "idx_pr_line_pr_id", columnList = "purchase_requisition_id"),
        @Index(name = "idx_pr_line_product", columnList = "product_id"),
        @Index(name = "idx_pr_line_fulfillment", columnList = "fulfillment_method")
})
@Getter
@Setter
@ToString
public class PurchaseRequisitionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_requisition_id", nullable = false)
    @ToString.Exclude
    private PurchaseRequisition purchaseRequisition;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber; // Sequential line number within PR

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_description", length = 500)
    private String productDescription; // Snapshot at time of requisition

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private UnitOfMeasure unitOfMeasure;

    // === QUANTITY TRACKING ===
    @Column(name = "requested_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedQuantity;

    @Column(name = "approved_quantity", precision = 19, scale = 4)
    private BigDecimal approvedQuantity; // Set during approval (may differ from requested)

    @Column(name = "ordered_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal orderedQuantity = BigDecimal.ZERO; // Linked to PO lines

    @Column(name = "fulfilled_from_stock_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal fulfilledFromStockQuantity = BigDecimal.ZERO;

    @Column(name = "fulfilled_from_transfer_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal fulfilledFromTransferQuantity = BigDecimal.ZERO;

    @Column(name = "remaining_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal remainingQuantity = BigDecimal.ZERO; // Calculated field

    // === PRICING (estimates for approval) ===
    @Column(name = "estimated_unit_price", precision = 19, scale = 4)
    private BigDecimal estimatedUnitPrice;

    @Column(name = "estimated_total_price", precision = 19, scale = 4)
    private BigDecimal estimatedTotalPrice;

    // === FULFILLMENT STRATEGY ===
    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_method", length = 30)
    private FulfillmentMethod fulfillmentMethod;

    @Column(name = "fulfillment_notes", columnDefinition = "TEXT")
    private String fulfillmentNotes; // Why this fulfillment method was chosen

    // === SPECIFICATIONS & REQUIREMENTS ===
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications; // Technical specs or requirements

    @Column(name = "preferred_brand", length = 100)
    private String preferredBrand;

    @Column(name = "is_substitute_acceptable")
    private Boolean isSubstituteAcceptable = true; // Can alternative product be used?

    // === APPROVAL ADJUSTMENTS ===
    @Column(name = "quantity_adjustment_reason", columnDefinition = "TEXT")
    private String quantityAdjustmentReason; // Why approved qty differs from requested

    // === AUDIT FIELDS ===
    @Column(name = "created_by_user_id")
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

    // === LIFECYCLE METHODS ===
    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;

        // Initialize quantities
        if (orderedQuantity == null) orderedQuantity = BigDecimal.ZERO;
        if (fulfilledFromStockQuantity == null) fulfilledFromStockQuantity = BigDecimal.ZERO;
        if (fulfilledFromTransferQuantity == null) fulfilledFromTransferQuantity = BigDecimal.ZERO;
        if (isSubstituteAcceptable == null) isSubstituteAcceptable = true;

        // Calculate estimated total if price is set
        calculateEstimatedTotal();

        // Calculate remaining quantity
        calculateRemainingQuantity();
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();

        // Recalculate on update
        calculateEstimatedTotal();
        calculateRemainingQuantity();
    }

    // === BUSINESS METHODS ===

    /**
     * Calculate estimated total price
     */
    public void calculateEstimatedTotal() {
        if (requestedQuantity != null && estimatedUnitPrice != null) {
            this.estimatedTotalPrice = requestedQuantity.multiply(estimatedUnitPrice);
        } else {
            this.estimatedTotalPrice = BigDecimal.ZERO;
        }
    }

    /**
     * Calculate remaining quantity to be fulfilled
     */
    public void calculateRemainingQuantity() {
        BigDecimal baseQuantity = approvedQuantity != null ? approvedQuantity : requestedQuantity;

        if (baseQuantity == null) {
            this.remainingQuantity = BigDecimal.ZERO;
            return;
        }

        BigDecimal totalFulfilled = getTotalFulfilledQuantity();
        this.remainingQuantity = baseQuantity.subtract(totalFulfilled);

        // Ensure not negative
        if (this.remainingQuantity.compareTo(BigDecimal.ZERO) < 0) {
            this.remainingQuantity = BigDecimal.ZERO;
        }
    }

    /**
     * Get total fulfilled quantity from all sources
     */
    public BigDecimal getTotalFulfilledQuantity() {
        BigDecimal total = BigDecimal.ZERO;

        if (orderedQuantity != null) {
            total = total.add(orderedQuantity);
        }
        if (fulfilledFromStockQuantity != null) {
            total = total.add(fulfilledFromStockQuantity);
        }
        if (fulfilledFromTransferQuantity != null) {
            total = total.add(fulfilledFromTransferQuantity);
        }

        return total;
    }

    /**
     * Check if line is fully fulfilled
     */
    public boolean isFullyFulfilled() {
        BigDecimal baseQuantity = approvedQuantity != null ? approvedQuantity : requestedQuantity;
        if (baseQuantity == null) {
            return false;
        }
        return getTotalFulfilledQuantity().compareTo(baseQuantity) >= 0;
    }

    /**
     * Check if line is partially fulfilled
     */
    public boolean isPartiallyFulfilled() {
        BigDecimal totalFulfilled = getTotalFulfilledQuantity();
        return totalFulfilled.compareTo(BigDecimal.ZERO) > 0 && !isFullyFulfilled();
    }

    /**
     * Record fulfillment from purchase order
     */
    public void recordPurchaseOrderFulfillment(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal newOrderedQty = this.orderedQuantity.add(quantity);
        BigDecimal baseQuantity = approvedQuantity != null ? approvedQuantity : requestedQuantity;

        // Don't exceed approved/requested quantity
        if (newOrderedQty.compareTo(baseQuantity) > 0) {
            newOrderedQty = baseQuantity;
        }

        this.orderedQuantity = newOrderedQty;
        calculateRemainingQuantity();
    }

    /**
     * Record fulfillment from stock
     */
    public void recordStockFulfillment(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal newFulfilledQty = this.fulfilledFromStockQuantity.add(quantity);
        BigDecimal baseQuantity = approvedQuantity != null ? approvedQuantity : requestedQuantity;

        // Don't exceed approved/requested quantity
        if (newFulfilledQty.compareTo(baseQuantity) > 0) {
            newFulfilledQty = baseQuantity;
        }

        this.fulfilledFromStockQuantity = newFulfilledQty;
        calculateRemainingQuantity();
    }

    /**
     * Record fulfillment from transfer
     */
    public void recordTransferFulfillment(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal newTransferQty = this.fulfilledFromTransferQuantity.add(quantity);
        BigDecimal baseQuantity = approvedQuantity != null ? approvedQuantity : requestedQuantity;

        // Don't exceed approved/requested quantity
        if (newTransferQty.compareTo(baseQuantity) > 0) {
            newTransferQty = baseQuantity;
        }

        this.fulfilledFromTransferQuantity = newTransferQty;
        calculateRemainingQuantity();
    }

    /**
     * Check if line can be fulfilled via purchase order
     */
    public boolean canBeFulfilledViaPurchase() {
        return fulfillmentMethod == FulfillmentMethod.PURCHASE &&
               remainingQuantity.compareTo(BigDecimal.ZERO) > 0;
    }
}
