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

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_return")
@Getter
@Setter
@ToString
public class PurchaseReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique reference number for the purchase return
    @Column(name = "return_number", unique = true, nullable = false)
    private String returnNumber;

    // Reference to the original Purchase Order
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    // Reference to the warehouse where the return originated
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_location_id", nullable = false)
    private WarehouseLocation warehouseLocation;

    // User who initiated the return
    @Column(name = "returned_by_user_id", nullable = false)
    private Long returnedByUserId;

    // Reason for the return
    @Column(name = "reason")
    private String reason;

    // Date the return was created
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }
}
