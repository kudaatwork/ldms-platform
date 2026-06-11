package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_reservations",
        indexes = {
                @Index(name = "idx_sales_reservation_status_until", columnList = "reservation_status,reserved_until"),
                @Index(name = "idx_sales_reservation_product_wh", columnList = "product_id,warehouse_location_id")
        })
@Getter
@Setter
@ToString
public class SalesReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_number", unique = true, nullable = false)
    private String reservationNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_location_id", nullable = false)
    @ToString.Exclude
    private WarehouseLocation warehouseLocation;

    @Column(name = "quantity_reserved", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantityReserved;

    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status")
    private ReservationStatus reservationStatus;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false)
    private EntityStatus entityStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (entityStatus == null) entityStatus = EntityStatus.ACTIVE;
        if (reservationStatus == null) reservationStatus = ReservationStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business method to check if reservation is expired
    public boolean isExpired() {
        return reservedUntil != null && LocalDateTime.now().isAfter(reservedUntil);
    }

    // Business method to check if reservation is active
    public boolean isActive() {
        return reservationStatus == ReservationStatus.ACTIVE && !isExpired();
    }

    // Helper method to get product ID
    public Long getProductId() {
        return product != null ? product.getId() : null;
    }

    // Helper method to get warehouse location ID
    public Long getWarehouseLocationId() {
        return warehouseLocation != null ? warehouseLocation.getId() : null;
    }
}