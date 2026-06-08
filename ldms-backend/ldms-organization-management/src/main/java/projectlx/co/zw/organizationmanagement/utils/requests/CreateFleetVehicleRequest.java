package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateFleetVehicleRequest {

    private String registration;
    private String makeModel;
    private String vehicleType;
    private String ownershipType;
    private Long contractedTransporterOrganizationId;
    private String status;
    private String driverName;
    private BigDecimal utilizationPct;
    private LocalDateTime lastTripAt;
}
