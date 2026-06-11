package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@ToString
public class WarehouseLocationMultipleFiltersRequest extends MultipleFiltersRequest {

    private String name;
    private String description;
    private EntityStatus entityStatus;
}
