package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoAllocateShipmentFromFleetRequest {

    private Long fleetAssetId;
    private Long fleetDriverId;
    private Long assetOrganizationId;
    private String ownershipType;
    private Long contractedTransporterOrganizationId;
}
