package projectlx.trip.tracking.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripLiveSnapshotDto {

    private Long tripId;
    private String tripNumber;
    private String status;
    private Long shipmentId;
    private String shipmentNumber;
    private String productName;
    private String productCode;
    private BigDecimal quantity;
    private String fromWarehouseName;
    private String toWarehouseName;
    private String vehicleRegistration;
    private String driverName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speedKmh;
    private BigDecimal headingDeg;
    private BigDecimal overallProgressPct;
    private boolean simulationActive;
    private boolean simulationPaused;
    private boolean moving;
    private boolean onBreak;
    private boolean speedLimitExceeded;
    private LocalDateTime recordedAt;
    private Long fleetAssetId;
    private BigDecimal maxSpeedKmh;
    private BigDecimal distanceTravelledKm;
    /** Distance advanced on this telemetry tick (simulation / ingest). */
    private BigDecimal distanceKmDelta;
    private BigDecimal fuelLevelPct;
    private BigDecimal fuelRemainingLiters;
    private List<TripRouteWaypointDto> routeWaypoints;
    private List<TripRouteWaypointDto> trail;

    private Integer currentSegmentIndex;
    private BigDecimal segmentProgressPct;
    private Integer completedWaypointCount;
    private Integer totalWaypointCount;

    private LocalDateTime journeyStartedAt;
    private Long totalElapsedSeconds;
    private Long transitSeconds;
    private Long waitingSeconds;
    private Long idleSeconds;
    private String journeyPhase;
    private Long estimatedArrivalSeconds;
}
