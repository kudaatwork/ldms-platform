package projectlx.fleet.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrganizationFleetDashboardDto {

    private long ownedFleetCount;
    private long contractedFleetCount;
    private long organizationDriverCount;
    private long contractedDriverCount;
}
