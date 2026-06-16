package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.inventory.management.model.WarehouseAccessLevel;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarehouseOrganizationAccessDto {

    private Long id;
    private Long warehouseLocationId;
    private Long grantedOrganizationId;
    private WarehouseAccessLevel accessLevel;
}
