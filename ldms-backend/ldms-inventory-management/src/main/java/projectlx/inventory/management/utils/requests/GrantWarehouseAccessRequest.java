package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.inventory.management.model.WarehouseAccessLevel;

@Getter
@Setter
public class GrantWarehouseAccessRequest {

    private Long warehouseLocationId;
    private Long grantedOrganizationId;
    private WarehouseAccessLevel accessLevel;
}
