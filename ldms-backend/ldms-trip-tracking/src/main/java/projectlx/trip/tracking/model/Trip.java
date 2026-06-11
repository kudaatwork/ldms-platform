package projectlx.trip.tracking.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip", indexes = {
        @Index(name = "idx_trip_org_status", columnList = "organization_id, entity_status"),
        @Index(name = "idx_trip_number", columnList = "trip_number"),
        @Index(name = "idx_trip_shipment", columnList = "shipment_id"),
        @Index(name = "idx_trip_status", columnList = "status, entity_status")
})
@Getter
@Setter
@ToString(exclude = {"tripEvents", "deliveryOtps"})
public class Trip implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_number", nullable = false, unique = true, length = 50)
    private String tripNumber;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // === LINKED RECORDS ===

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(name = "inventory_transfer_id")
    private Long inventoryTransferId;

    // === FLEET ASSIGNMENT ===

    @Column(name = "fleet_driver_id")
    private Long fleetDriverId;

    @Column(name = "fleet_asset_id")
    private Long fleetAssetId;

    // === STATUS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TripStatus status = TripStatus.SCHEDULED;

    // === TIMESTAMPS ===

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // === DELIVERY ===

    @Column(name = "receiver_user_id")
    private Long receiverUserId;

    // === DENORMALISED DISPLAY FIELDS ===

    @Column(name = "from_warehouse_name", length = 200)
    private String fromWarehouseName;

    @Column(name = "to_warehouse_name", length = 200)
    private String toWarehouseName;

    @Column(name = "product_name", length = 500)
    private String productName;

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

    // === RELATIONSHIPS ===

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripEvent> tripEvents = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryOtp> deliveryOtps = new ArrayList<>();
}
