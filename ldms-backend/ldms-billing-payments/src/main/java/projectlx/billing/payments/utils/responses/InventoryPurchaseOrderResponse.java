package projectlx.billing.payments.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.dtos.InventoryPurchaseOrderDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryPurchaseOrderResponse extends CommonResponse {
    private InventoryPurchaseOrderDto purchaseOrderDto;
}
