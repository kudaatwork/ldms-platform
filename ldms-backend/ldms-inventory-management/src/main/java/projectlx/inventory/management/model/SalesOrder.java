package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales Order - Supplier's view of a customer Purchase Order
 *
 * BACK-TO-BACK FULFILLMENT LIFECYCLE:
 * 1. Created automatically when PO is APPROVED (event: po.approved)
 * 2. Status: AWAITING_RECEIPT (stock doesn't exist yet)
 * 3. Goods received (GRV created) → PENDING (stock now exists)
 * 4. Supplier confirms → CONFIRMED → Stock is RESERVED ✅
 * 5. Dispatch → PARTIALLY_SHIPPED / SHIPPED
 * 6. Delivery → DELIVERED → FULFILLED
 *
 * KEY RULE: SO cannot be confirmed until goods are physically received.
 */
@Entity
@Table(name = "sales_order", indexes = {
        @Index(name = "ux_sales_order_sales_order_number", columnList = "sales_order_number", unique = true),
        @Index(name = "idx_sales_order_purchase_order_id", columnList = "purchase_order_id"),
        @Index(name = "idx_sales_order_supplier_org_id", columnList = "supplier_organization_id")
})
@Getter
@Setter
@ToString
public class SalesOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_order_number", nullable = false)
    private String salesOrderNumber;

    // ========================================
    // NEW: PO → SO Linkage Fields
    // ========================================

    /**
     * Reference to the originating Purchase Order
     * This links the customer's PO to the supplier's SO
     */
    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    /**
     * Copy of PO number for traceability and display
     */
    @Column(name = "purchase_order_number", nullable = false)
    private String purchaseOrderNumber;

    /**
     * The supplier organization fulfilling this order
     * (In multi-tenant scenario, this identifies which supplier)
     */
    @Column(name = "supplier_organization_id", nullable = false)
    private Long supplierOrganizationId;

    /**
     * Warehouse from which goods will be fulfilled
     * Set during SO confirmation, used for stock reservation
     */
    @Column(name = "fulfillment_warehouse_id")
    private Long fulfillmentWarehouseId;

    /**
     * When supplier confirmed they can fulfill (status → CONFIRMED)
     * This is when stock reservation happens
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * User who confirmed the SO on supplier side
     */
    @Column(name = "confirmed_by_user_id")
    private Long confirmedByUserId;

    // ========================================
    // Existing Fields
    // ========================================

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrderStatus status;

    @Column(name = "current_approval_stage", nullable = false)
    private Integer currentApprovalStage = 0;

    @Column(name = "required_approval_stages")
    private Integer requiredApprovalStages;

    @Column(name = "approval_complete", nullable = false)
    private Boolean approvalComplete = false;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "inventory_reserved_at_shipment", nullable = false)
    private Boolean inventoryReservedAtShipment = true;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_term", length = 50)
    private PaymentTerm paymentTerm;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    /**
     * Reference to the Shipment handling this order's delivery
     * Set when Shipment Management Service creates dispatch
     */
    @Column(name = "shipment_id")
    private Long shipmentId;  // ← NEW FIELD

    /**
     * When shipment was created/assigned
     */
    @Column(name = "shipment_created_at")
    private LocalDateTime shipmentCreatedAt;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<SalesOrderLine> salesOrderLines = new ArrayList<>();

    public void create() {
        super.create();
        if (status == null) status = SalesOrderStatus.PENDING;
        if (orderDate == null) orderDate = LocalDate.now();
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
    }

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Check if this SO is linked to a PO
     */
    public boolean isLinkedToPurchaseOrder() {
        return purchaseOrderId != null;
    }

    /**
     * Check if SO is awaiting goods receipt (Back-to-Back flow)
     * In this state, the SO cannot be confirmed because stock doesn't exist yet.
     */
    public boolean isAwaitingReceipt() {
        return status == SalesOrderStatus.AWAITING_RECEIPT;
    }

    /**
     * Check if SO is ready for confirmation (goods have been received)
     */
    public boolean isReadyForConfirmation() {
        return status == SalesOrderStatus.PENDING;
    }

    /**
     * Check if SO can be confirmed (must not be awaiting receipt)
     */
    public boolean canBeConfirmed() {
        return status == SalesOrderStatus.PENDING;
    }

    public boolean canBeApprovedForShipment() {
        return status == SalesOrderStatus.PENDING_APPROVAL;
    }

    public boolean isReadyToShip() {
        return status == SalesOrderStatus.APPROVED
                || status == SalesOrderStatus.CONFIRMED
                || status == SalesOrderStatus.PARTIALLY_SHIPPED;
    }

    /**
     * Check if SO is confirmed (and stock should be reserved)
     */
    public boolean isConfirmed() {
        return status == SalesOrderStatus.APPROVED
                || status == SalesOrderStatus.CONFIRMED
                || status == SalesOrderStatus.PARTIALLY_SHIPPED
                || status == SalesOrderStatus.SHIPPED
                || status == SalesOrderStatus.DELIVERED
                || status == SalesOrderStatus.FULFILLED;
    }

    /**
     * Check if fulfillment warehouse is set
     */
    public boolean hasFulfillmentWarehouse() {
        return fulfillmentWarehouseId != null;
    }
}
