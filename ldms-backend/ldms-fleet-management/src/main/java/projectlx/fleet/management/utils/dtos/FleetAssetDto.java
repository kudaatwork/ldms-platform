package projectlx.fleet.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FleetAssetDto {
    private Long id;
    private Long organizationId;
    private String assetType;
    private String ownershipType;
    private Long contractedTransporterOrganizationId;
    private String contractScope;
    private String jobReference;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String registrationStatus;
    private String registration;
    private String makeModel;
    private String status;
    private String driverName;
    private Long fleetDriverId;
    private BigDecimal utilizationPct;
    private BigDecimal maxSpeedKmh;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
