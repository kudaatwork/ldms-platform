package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class EditFleetAssetRequest {
    private Long id;
    private String assetType;
    private String ownershipType;
    private Long contractedTransporterOrganizationId;
    private String contractScope;
    private String jobReference;
    private String contractStartDate;
    private String contractEndDate;
    private String registration;
    private String makeModel;
    private String status;
    private String driverName;
    private Long fleetDriverId;
    private BigDecimal utilizationPct;
    private BigDecimal maxSpeedKmh;
}
