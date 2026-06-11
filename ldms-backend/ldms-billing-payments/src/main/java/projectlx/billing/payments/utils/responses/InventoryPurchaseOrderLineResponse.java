package projectlx.billing.payments.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.dtos.InventoryPurchaseOrderLineDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryPurchaseOrderLineResponse extends CommonResponse {
    private List<InventoryPurchaseOrderLineDto> purchaseOrderLineDtoList;
}
