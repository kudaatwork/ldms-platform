package projectlx.co.zw.organizationmanagement.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetVehicleDto {

    private Long id;
    private Long organizationId;
    private String ownershipType;
    private Long contractedTransporterOrganizationId;
    private String contractedTransporterOrganizationName;
    private String registration;
    private String makeModel;
    private String vehicleType;
    private String status;
    private String driverName;
    private BigDecimal utilizationPct;
    private LocalDateTime lastTripAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
