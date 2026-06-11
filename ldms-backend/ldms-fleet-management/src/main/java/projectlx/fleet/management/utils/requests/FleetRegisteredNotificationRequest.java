package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FleetRegisteredNotificationRequest {

    private Long registeringOrganizationId;
    private Long contractedTransporterOrganizationId;
    private String ownershipType;
    private String registration;
    private String makeModel;
    private String assetType;
    private String performedBy;
}
