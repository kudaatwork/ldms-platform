package projectlx.fleet.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformFleetDashboardDto {

    private long totalFleetAssets;
    private long ownedFleetAssets;
    private long contractedFleetAssets;
    private long totalDrivers;
    private long organizationsWithFleet;
}
