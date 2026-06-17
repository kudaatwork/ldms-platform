package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class PlatformOrganizationTripStatsDto {

    private Long organizationId;
    private long activeTrips;
}
