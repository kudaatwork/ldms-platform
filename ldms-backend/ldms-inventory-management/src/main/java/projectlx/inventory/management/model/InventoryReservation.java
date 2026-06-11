package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations",
        indexes = {
                @Index(name = "idx_invres_sales_order", columnList = "sales_order_id"),
                @Index(name = "idx_invres_order_line", columnList = "sales_order_line_id"),
                @Index(name = "idx_invres_product_location", columnList = "product_id, warehouse_location_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invres_line_product_wh", columnNames = {"sales_order_line_id", "product_id", "warehouse_location_id"})
        })
@Getter
@Setter
@ToString
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_order_id", nullable = false)
    private Long salesOrderId;

    @Column(name = "sales_order_line_id", nullable = false)
    private Long salesOrderLineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_location_id", nullable = false)
    @ToString.Exclude
    private WarehouseLocation warehouseLocation;

    @Column(name = "quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = ReservationStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getProductId() { return product != null ? product.getId() : null; }
    public Long getWarehouseLocationId() { return warehouseLocation != null ? warehouseLocation.getId() : null; }
}
