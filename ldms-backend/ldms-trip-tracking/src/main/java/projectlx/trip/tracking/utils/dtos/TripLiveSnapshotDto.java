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
    private boolean moving;
    private LocalDateTime recordedAt;
    private List<TripRouteWaypointDto> routeWaypoints;
    private List<TripRouteWaypointDto> trail;
}
