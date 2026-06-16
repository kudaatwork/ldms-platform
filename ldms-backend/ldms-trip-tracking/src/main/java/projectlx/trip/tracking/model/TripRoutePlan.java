package projectlx.trip.tracking.model;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_route_plan", indexes = {
        @Index(name = "idx_trip_route_plan_org", columnList = "organization_id, entity_status")
})
@Getter
@Setter
@ToString
public class TripRoutePlan implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "origin_label", length = 200)
    private String originLabel;

    @Column(name = "destination_label", length = 200)
    private String destinationLabel;

    @Column(name = "origin_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(name = "destination_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLatitude;

    @Column(name = "destination_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLongitude;

    @Column(name = "waypoints_json", nullable = false, columnDefinition = "JSON")
    private String waypointsJson;

    @Column(name = "total_distance_km", precision = 10, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "simulation_active", nullable = false)
    private boolean simulationActive;

    @Column(name = "current_segment_index", nullable = false)
    private int currentSegmentIndex;

    @Column(name = "segment_progress_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal segmentProgressPct = BigDecimal.ZERO;

    @Column(name = "overall_progress_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallProgressPct = BigDecimal.ZERO;

    @Column(name = "current_latitude", precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    @Column(name = "current_speed_kmh", precision = 6, scale = 2)
    private BigDecimal currentSpeedKmh;

    @Column(name = "current_heading_deg", precision = 6, scale = 2)
    private BigDecimal currentHeadingDeg;

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
