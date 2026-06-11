package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@ToString
public class EditPurchaseReturnRequest {
    private Long purchaseReturnId;
    private Long purchaseOrderId;
    private Long warehouseLocationId;
    private Long returnedByUserId;
    private String reason;
    private EntityStatus entityStatus;
}