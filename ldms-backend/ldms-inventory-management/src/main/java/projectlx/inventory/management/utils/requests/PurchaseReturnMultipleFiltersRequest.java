package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class PurchaseReturnMultipleFiltersRequest extends MultipleFiltersRequest {
    private Long purchaseReturnId;
    private String returnNumber;
    private Long purchaseOrderId;
    private Long warehouseLocationId;
    private Long returnedByUserId;
    private EntityStatus entityStatus;
}