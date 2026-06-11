package projectlx.shipment.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.shipment.management.utils.enums.ShipmentSourceType;
import projectlx.shipment.management.utils.enums.ShipmentStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment", indexes = {
        @Index(name = "idx_shipment_org_id", columnList = "organization_id"),
        @Index(name = "idx_shipment_transfer_id", columnList = "inventory_transfer_id"),
        @Index(name = "idx_shipment_status", columnList = "status, entity_status")
})
@Getter
@Setter
@ToString
public class Shipment implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_number", nullable = false, unique = true, length = 50)
    private String shipmentNumber;

    // === ORGANISATION & SOURCE ===

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private ShipmentSourceType sourceType;

    @Column(name = "inventory_transfer_id", nullable = false)
    private Long inventoryTransferId;

    // === WAREHOUSE LOCATIONS ===

    @Column(name = "from_warehouse_location_id")
    private Long fromWarehouseLocationId;

    @Column(name = "to_warehouse_location_id")
    private Long toWarehouseLocationId;

    @Column(name = "from_warehouse_name", length = 200)
    private String fromWarehouseName;

    @Column(name = "to_warehouse_name", length = 200)
    private String toWarehouseName;

    // === PRODUCT ===

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_code", length = 60)
    private String productCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    // === FLEET ALLOCATION ===

    @Column(name = "fleet_driver_id")
    private Long fleetDriverId;

    @Column(name = "fleet_asset_id")
    private Long fleetAssetId;

    // === TRIP LINK ===

    @Column(name = "trip_id")
    private Long tripId;

    // === STATUS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ShipmentStatus status = ShipmentStatus.PENDING_ALLOCATION;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === AUDIT ===

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;
}
