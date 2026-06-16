package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.WarehouseAccessLevel;
import projectlx.inventory.management.model.WarehouseLocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarehouseLocationDto {

    private Long id;

    private String locationId;
    private Long supplierId;
    private Long branchId;
    private WarehouseLocationType warehouseType;
    private Boolean virtualWarehouse;
    private String name;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;

    /** True when the signed-in organisation owns this warehouse. */
    private Boolean organizationOwned;

    /** True when the warehouse is shared with (not owned by) the signed-in organisation. */
    private Boolean sharedAccess;

    /** Access level for the signed-in organisation when {@link #sharedAccess} is true. */
    private WarehouseAccessLevel callerAccessLevel;
}
