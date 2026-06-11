package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single product and quantity within a purchase order.
 * This is a child entity of the PurchaseOrder.
 */
@Entity
@Table
@Getter
@Setter
@ToString
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    // === PURCHASE REQUISITION REFERENCE ===
    @Column(name = "purchase_requisition_line_id")
    private Long purchaseRequisitionLineId; // Source PR line if created from PR

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // External reference to supplier's product code, if available
    @Column(name = "supplier_product_code")
    private String supplierProductCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private UnitOfMeasure unitOfMeasure;

    // Using BigDecimal for quantity for better precision
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitPrice;

    // Calculated field for total cost of the line
    @Column(name = "total_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "unit_price_functional", precision = 19, scale = 4)
    private BigDecimal unitPriceFunctional;

    @Column(name = "total_price_functional", precision = 19, scale = 4)
    private BigDecimal totalPriceFunctional;

    @Column(name = "exchange_rate_snapshot_id")
    private Long exchangeRateSnapshotId;

    // Using BigDecimal for received quantity for consistency and precision
    @Column(name = "received_quantity", precision = 19, scale = 4)
    private BigDecimal receivedQuantity;

    // New audit fields
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;
        if (receivedQuantity == null) {
            receivedQuantity = BigDecimal.ZERO;
        }
        // Calculate total price on creation
        if (quantity != null && unitPrice != null) {
            this.totalPrice = quantity.multiply(unitPrice);
        }
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
        // Recalculate total price on update
        if (quantity != null && unitPrice != null) {
            this.totalPrice = quantity.multiply(unitPrice);
        }
    }
}
