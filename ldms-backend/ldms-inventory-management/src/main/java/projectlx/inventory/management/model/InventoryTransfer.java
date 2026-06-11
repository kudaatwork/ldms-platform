package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transfer", indexes = {
        @Index(name = "ux_inventory_transfer_transfer_number", columnList = "transfer_number", unique = true)
})
@Getter
@Setter
@ToString
public class InventoryTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A unique, human-readable identifier for the transfer
    @Column(name = "transfer_number", nullable = false)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id")
    private WarehouseLocation fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id")
    private WarehouseLocation toLocation;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    // NEW FIELD: To track the cost of the transferred items, for WAC calculation
    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status;

    @Column(name = "reference")
    private String reference;

    // Set when this transfer is linked to a shipment/trip in the logistics service
    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

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
        if (status == null) status = TransferStatus.REQUESTED;
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }
}
