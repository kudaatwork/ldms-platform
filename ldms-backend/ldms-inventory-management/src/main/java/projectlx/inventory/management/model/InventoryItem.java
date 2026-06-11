package projectlx.inventory.management.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_inventory_item_product", columnList = "product_id"),
        @Index(name = "idx_inventory_item_warehouse", columnList = "warehouse_location_id"),
        @Index(name = "idx_inventory_item_product_wh", columnList = "product_id,warehouse_location_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_item_product_wh", columnNames = {"product_id", "warehouse_location_id"})
})
@Getter
@Setter
@ToString
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_location_id")
    private WarehouseLocation warehouseLocation;

    @Column(name = "current_stock", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost = BigDecimal.ZERO;

    /** Optional: last purchase price, for reference even when stock = 0 */
    @Column(name = "last_purchase_cost", precision = 19, scale = 4)
    private BigDecimal lastPurchaseCost = BigDecimal.ZERO;

    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 19, scale = 4)
    private BigDecimal minStockLevel = BigDecimal.ZERO;

    @Column(name = "reorder_quantity", precision = 19, scale = 4)
    private BigDecimal reorderQuantity = BigDecimal.ZERO;

    @Column(name = "batch_lot")
    private String batchLot;

    @Column(name = "serial_number")
    private String serialNumber;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<StockTransactionHistory> transactions;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    // Helper method to get the average cost for WAC
    public BigDecimal getAverageCost() {
        if (averageCost != null) return averageCost;
        if (getCurrentStock() != null && getCurrentStock().compareTo(BigDecimal.ZERO) > 0 &&
                getTotalCost() != null) {
            return getTotalCost().divide(getCurrentStock(), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getAvailableQuantity() {
        BigDecimal current = getCurrentStock() != null ? getCurrentStock() : BigDecimal.ZERO;
        BigDecimal reserved = getReservedQuantity() != null ? getReservedQuantity() : BigDecimal.ZERO;
        BigDecimal available = current.subtract(reserved);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    // Helper method to check if sufficient stock is available for allocation
    @Transient
    public boolean hasAvailableStock(BigDecimal requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return getAvailableQuantity().compareTo(requiredQuantity) >= 0;
    }

    // Helper method to safely add to reserved quantity
    public void addReservedQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal current = this.reservedQuantity != null ? this.reservedQuantity : BigDecimal.ZERO;
        BigDecimal newReserved = current.add(quantity);

        // Validate that we don't reserve more than current stock
        if (newReserved.compareTo(this.currentStock) > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot reserve %s. Would exceed current stock of %s",
                            quantity, this.currentStock));
        }

        this.reservedQuantity = newReserved;
    }

    // Helper method to safely subtract from reserved quantity
    public void releaseReservedQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal current = this.reservedQuantity != null ? this.reservedQuantity : BigDecimal.ZERO;
        BigDecimal newReserved = current.subtract(quantity);

        // Ensure reserved quantity doesn't go negative
        this.reservedQuantity = newReserved.max(BigDecimal.ZERO);
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();

        // Validate business rules on update
        validateReservedQuantity();
    }

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;

        // Initialize default values
        if (this.quantity == null) {
            this.quantity = BigDecimal.ZERO;
        }
        if (this.totalCost == null) {
            this.totalCost = BigDecimal.ZERO;
        }
        if (this.currentStock == null) {
            this.currentStock = BigDecimal.ZERO;
        }
        if (this.unitCost == null) {
            this.unitCost = BigDecimal.ZERO;
        }
        if (this.reservedQuantity == null) {
            this.reservedQuantity = BigDecimal.ZERO;
        }
        if (this.averageCost == null) {
            this.averageCost = BigDecimal.ZERO;
        }
        if (this.minStockLevel == null) {
            this.minStockLevel = BigDecimal.ZERO;
        }

        // Validate business rules on creation
        validateReservedQuantity();
    }

    // Private method to validate reserved quantity constraints
    private void validateReservedQuantity() {

        if (this.reservedQuantity != null && this.currentStock != null) {

            if (this.reservedQuantity.compareTo(this.currentStock) > 0) {
                throw new IllegalStateException(
                        String.format("Reserved quantity (%s) cannot exceed current stock (%s)",
                                this.reservedQuantity, this.currentStock));
            }

            if (this.reservedQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Reserved quantity cannot be negative");
            }
        }
    }
}