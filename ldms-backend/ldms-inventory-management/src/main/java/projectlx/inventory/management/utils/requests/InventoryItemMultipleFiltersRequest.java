package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.utils.enums.StockLevelStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class InventoryItemMultipleFiltersRequest extends MultipleFiltersRequest {

    private Long productId;
    private Long warehouseLocationId;
    private Long supplierId;
    private String batchLot;
    private String serialNumber;
    private EntityStatus entityStatus;
    private StockLevelStatus stockStatus;
}
