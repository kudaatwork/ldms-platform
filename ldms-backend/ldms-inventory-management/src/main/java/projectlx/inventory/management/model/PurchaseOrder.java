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

@Entity
@Table(indexes = {
        @Index(name = "ux_purchase_order_purchase_order_number", columnList = "purchase_order_number", unique = true),
        @Index(name = "idx_po_supplier_status", columnList = "supplier_id, status"),
        @Index(name = "idx_po_order_date", columnList = "order_date")
})
@Getter
@Setter
@ToString
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_number", nullable = false, unique = true, length = 50)
    private String purchaseOrderNumber;

    @Column(name = "external_id", length = 100)
    private String externalId;

    // === PURCHASE REQUISITION REFERENCE ===
    @Column(name = "purchase_requisition_id")
    private Long purchaseRequisitionId; // Source PR if auto-created

    // === PARTY INFORMATION ===
    @Column(name = "organization_id", nullable = false)
    private Long organizationId; // Your company making the purchase

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "buyer_contact", nullable = false, length = 200)
    private String buyerContact;

    @Column(name = "supplier_contact", nullable = false, length = 200)
    private String supplierContact;

    // === FINANCIAL TERMS ===
    @Column(name = "currency", nullable = false, length = 3)
    private String currency; // Transaction currency (ISO 4217)

    @Column(name = "functional_currency_code", length = 3)
    private String functionalCurrencyCode;

    @Column(name = "exchange_rate_snapshot_id")
    private Long exchangeRateSnapshotId;

    @Column(name = "exchange_rate_used", precision = 19, scale = 8)
    private BigDecimal exchangeRateUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_term")
    private PaymentTerm paymentTerm;

    @Column(name = "payment_due_date", nullable = false)
    private LocalDate paymentDueDate;

    // Optional early payment incentive
    @Column(name = "early_payment_discount_pct", precision = 5, scale = 2)
    private BigDecimal earlyPaymentDiscountPct;

    @Column(name = "early_payment_discount_until")
    private LocalDate earlyPaymentDiscountUntil;

    // Optional prepayment requirement
    @Column(name = "prepayment_required")
    private Boolean prepaymentRequired = false;

    @Column(name = "prepayment_percent", precision = 5, scale = 2)
    private BigDecimal prepaymentPercent;

    // === AMOUNTS ===
    @Column(name = "subtotal", precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "subtotal_functional", precision = 19, scale = 4)
    private BigDecimal subtotalFunctional;

    @Column(name = "tax_amount_functional", precision = 19, scale = 4)
    private BigDecimal taxAmountFunctional;

    @Column(name = "total_amount_functional", precision = 19, scale = 4)
    private BigDecimal totalAmountFunctional;

    // === SHIPPING & LOGISTICS ===
    @Column(name = "ship_from_location_id", nullable = false)
    private Long shipFromLocationId;

    @Column(name = "ship_to_location_id", nullable = false)
    private Long shipToLocationId;

    @Column(name = "receiving_warehouse_id", nullable = false)
    private Long receivingWarehouseId; // Where inventory will be received

    @Enumerated(EnumType.STRING)
    @Column(name = "freight_terms")
    private FreightTerms freightTerms; // FOB, CIF, EXW, DDP

    @Enumerated(EnumType.STRING)
    @Column(name = "ship_mode", nullable = false)
    private ShipMode shipMode;

    @Column(name = "shipping_instructions", columnDefinition = "TEXT")
    private String shippingInstructions;

    // === IMPORT/EXPORT (for cross-border) ===
    @Column(name = "is_import")
    private Boolean isImport = false;

    @Column(name = "customs_declaration_number", length = 100)
    private String customsDeclarationNumber;

    @Column(name = "port_of_entry", length = 100)
    private String portOfEntry;

    // === STATUS & WORKFLOW ===
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderStatus status;

    @Column(name = "current_customer_approval_stage", nullable = false)
    private Integer currentCustomerApprovalStage = 0;

    @Column(name = "current_supplier_approval_stage", nullable = false)
    private Integer currentSupplierApprovalStage = 0;

    @Column(name = "required_approval_stages")
    private Integer requiredApprovalStages;

    @Column(name = "customer_approval_complete", nullable = false)
    private Boolean customerApprovalComplete = false;

    @Column(name = "supplier_approval_complete", nullable = false)
    private Boolean supplierApprovalComplete = false;

    @Column(name = "customer_approved_at")
    private LocalDateTime customerApprovedAt;

    @Column(name = "supplier_approved_at")
    private LocalDateTime supplierApprovedAt;

    @Column(name = "payment_confirmed", nullable = false)
    private Boolean paymentConfirmed = false;

    @Column(name = "payment_confirmed_at")
    private LocalDateTime paymentConfirmedAt;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    // Approval workflow
    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

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

    // === NOTES ===
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === RELATIONSHIPS ===
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PurchaseOrderLine> purchaseOrderLines = new ArrayList<>();

    // === LIFECYCLE METHODS ===
    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;

        // Set defaults
        if (status == null) status = PurchaseOrderStatus.DRAFT;
        if (orderDate == null) orderDate = LocalDate.now();
        if (shipMode == null) shipMode = ShipMode.ROAD;
        if (isImport == null) isImport = false;
        if (prepaymentRequired == null) prepaymentRequired = false;

        // Initialize amounts
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }

    // === BUSINESS METHODS ===

    /**
     * Calculates totals from purchase order lines
     */
    public void calculateTotals() {
        if (purchaseOrderLines == null || purchaseOrderLines.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
            this.taxAmount = BigDecimal.ZERO;
            this.totalAmount = BigDecimal.ZERO;
            return;
        }

        // Sum up line totals
        this.subtotal = purchaseOrderLines.stream()
                .map(line -> {
                    if (line.getTotalPrice() != null) {
                        return line.getTotalPrice();
                    }
                    if (line.getQuantity() != null && line.getUnitPrice() != null) {
                        return line.getQuantity().multiply(line.getUnitPrice());
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate tax
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = subtotal.multiply(taxRate).divide(new BigDecimal("100"));
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        // Calculate total
        this.totalAmount = subtotal.add(taxAmount);
    }

    /**
     * Check if PO is fully received
     */
    public boolean isFullyReceived() {
        if (purchaseOrderLines == null || purchaseOrderLines.isEmpty()) {
            return false;
        }
        return purchaseOrderLines.stream()
                .allMatch(line -> line.getReceivedQuantity().compareTo(line.getQuantity()) >= 0);
    }

    /**
     * Check if PO requires approval
     */
    public boolean requiresApproval() {
        // Example: POs over certain amount need approval
        // Customize this logic based on your business rules
        return totalAmount != null && totalAmount.compareTo(new BigDecimal("10000")) > 0;
    }
}
