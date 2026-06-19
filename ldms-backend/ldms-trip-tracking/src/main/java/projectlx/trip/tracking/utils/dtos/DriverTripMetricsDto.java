package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverTripMetricsDto {

    private int activeTrips;
    private int completedToday;
    private int pendingDeliveries;
}
