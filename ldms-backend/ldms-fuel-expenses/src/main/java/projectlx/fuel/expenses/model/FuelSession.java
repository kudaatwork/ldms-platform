package projectlx.fuel.expenses.model;

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
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fuel.expenses.utils.enums.FuelSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_session", indexes = {
        @Index(name = "idx_fuel_session_org_status",  columnList = "organization_id, entity_status"),
        @Index(name = "idx_fuel_session_trip",        columnList = "trip_id"),
        @Index(name = "idx_fuel_session_fleet_asset", columnList = "fleet_asset_id"),
        @Index(name = "idx_fuel_session_status",      columnList = "status, entity_status")
})
@Getter
@Setter
@ToString
public class FuelSession implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === TRIP CONTEXT ===

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "fleet_asset_id")
    private Long fleetAssetId;

    @Column(name = "fleet_driver_id")
    private Long fleetDriverId;

    @Column(name = "shipment_id")
    private Long shipmentId;

    // === FUEL METRICS ===

    @Column(name = "tank_capacity_liters", nullable = false, precision = 19, scale = 2)
    private BigDecimal tankCapacityLiters = new BigDecimal("400.00");

    @Column(name = "fuel_remaining_liters", nullable = false, precision = 19, scale = 2)
    private BigDecimal fuelRemainingLiters = new BigDecimal("400.00");

    @Column(name = "fuel_level_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal fuelLevelPct = new BigDecimal("100.00");

    @Column(name = "consumption_rate_l_per_100km", nullable = false, precision = 6, scale = 2)
    private BigDecimal consumptionRateLPer100km = new BigDecimal("35.00");

    @Column(name = "distance_travelled_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceTravelledKm = BigDecimal.ZERO;

    // === GPS ===

    @Column(name = "last_latitude", precision = 10, scale = 7)
    private BigDecimal lastLatitude;

    @Column(name = "last_longitude", precision = 10, scale = 7)
    private BigDecimal lastLongitude;

    // === STATUS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FuelSessionStatus status = FuelSessionStatus.ACTIVE;

    @Column(name = "moving", nullable = false)
    private boolean moving = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    // === AUDIT ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;
}
