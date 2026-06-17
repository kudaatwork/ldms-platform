package projectlx.shipment.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class PlatformShipmentDashboardDto {

    private long activeShipments;
    private long completedThisMonth;
    private long organizationsWithActivity;
    private List<PlatformOrganizationShipmentStatsDto> organizationStats = new ArrayList<>();
    private List<PlatformShipmentStatusCountDto> shipmentsByStatus = new ArrayList<>();
    private List<Long> weeklyVolume = new ArrayList<>();
    private List<ShipmentDto> liveShipments = new ArrayList<>();
}
