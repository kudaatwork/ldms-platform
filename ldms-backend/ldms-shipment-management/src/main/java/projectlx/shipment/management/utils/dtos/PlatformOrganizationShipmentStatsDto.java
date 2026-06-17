package projectlx.shipment.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class PlatformOrganizationShipmentStatsDto {

    private Long organizationId;
    private long activeShipments;
    private long completedThisMonth;
    private LocalDateTime lastActivityAt;
}
