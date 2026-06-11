package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class StockAdjustmentMultipleFiltersRequest extends MultipleFiltersRequest {

    private Long inventoryItemId;
    private String reason;
    private Long adjustedByUserId;
    private EntityStatus entityStatus;
}