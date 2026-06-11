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
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "goods_received_voucher", indexes = {
        @Index(name = "ux_grv_grv_number", columnList = "grv_number", unique = true)
})
@Getter
@Setter
@ToString
public class GoodsReceivedVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A unique, human-readable identifier for the GRV
    @Column(name = "grv_number", nullable = false)
    private String grvNumber;

    // Either purchaseOrder or inventoryTransfer must be set (but not both mandatory)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "purchase_order_id", nullable = true)
    private PurchaseOrder purchaseOrder;

    // Set when GRV is created from a transfer completion (no PO required)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "inventory_transfer_id", nullable = true)
    private InventoryTransfer inventoryTransfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_location_id", nullable = false)
    private WarehouseLocation warehouseLocation;

    @Column(name = "received_by_user_id", nullable = false)
    private Long receivedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GrvStatus status; // or GrvStatus status

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "notes")
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        receivedDate = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }
}
