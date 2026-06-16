package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.WarehouseLocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditWarehouseLocationRequest {

    // Identifier
    private Long warehouseLocationId;

    private String name;
    private String description;

    // Editable fields
    private String locationId;
    private Long supplierId;
    private Long branchId;
    private WarehouseLocationType warehouseType;
}
