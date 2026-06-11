package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryPurchaseOrderLineDto {
    private Long id;
    private Long purchaseOrderId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal unitPriceFunctional;
    private BigDecimal totalPriceFunctional;
}
