package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryPurchaseOrderLineFilterRequest {
    private Long purchaseOrderId;
    private Integer page;
    private Integer size;
}
