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
import projectlx.fuel.expenses.utils.enums.FuelReadingType;
import projectlx.fuel.expenses.utils.enums.FuelTelemetrySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_telemetry_log", indexes = {
        @Index(name = "idx_ftl_trip_id",      columnList = "trip_id"),
        @Index(name = "idx_ftl_fuel_session", columnList = "fuel_session_id"),
        @Index(name = "idx_ftl_org_status",   columnList = "organization_id, entity_status"),
        @Index(name = "idx_ftl_fleet_asset",  columnList = "fleet_asset_id"),
        @Index(name = "idx_ftl_source_type",  columnList = "source, reading_type"),
        @Index(name = "idx_ftl_recorded_at",  columnList = "recorded_at")
})
@Getter
@Setter
@ToString
public class FuelTelemetryLog implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === CONTEXT ===

    @Column(name = "fuel_session_id")
    private Long fuelSessionId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "fleet_asset_id")
    private Long fleetAssetId;

    // === CLASSIFICATION ===

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private FuelTelemetrySource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", nullable = false, length = 50)
    private FuelReadingType readingType;

    // === MEASUREMENTS ===

    @Column(name = "fuel_level_pct", precision = 5, scale = 2)
    private BigDecimal fuelLevelPct;

    @Column(name = "fuel_liters", precision = 19, scale = 2)
    private BigDecimal fuelLiters;

    @Column(name = "odometer_km", precision = 10, scale = 2)
    private BigDecimal odometerKm;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "distance_delta_km", precision = 10, scale = 4)
    private BigDecimal distanceDeltaKm;

    @Column(name = "consumed_liters", precision = 19, scale = 2)
    private BigDecimal consumedLiters;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    // === SOFT DELETE ===

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
