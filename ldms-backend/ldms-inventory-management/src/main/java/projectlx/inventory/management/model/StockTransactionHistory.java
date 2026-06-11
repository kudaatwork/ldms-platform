package projectlx.inventory.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transaction_history")
@Getter
@Setter
@ToString
public class StockTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    // Using BigDecimal for quantity to prevent floating-point rounding errors
    @Column(name = "quantity_change", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityChange;

    // NEW FIELD: Cost per unit at the time of the transaction.
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_location_id")
    private WarehouseLocation warehouseLocation;

    // A generic field to track the user who performed the transaction
    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    // A reference to the specific business document that triggered this transaction
    @Column(name = "reference_document_id")
    private Long referenceDocumentId;

    // A reference to the type of business document, e.g., PurchaseOrder, SalesOrder
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_document_type")
    private ReferenceDocumentType referenceDocumentType;

    @Column(name = "reason")
    private String reason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
