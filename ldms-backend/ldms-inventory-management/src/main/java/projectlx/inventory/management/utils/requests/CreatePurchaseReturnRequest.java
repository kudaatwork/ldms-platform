package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
public class CreatePurchaseReturnRequest {
    private Long purchaseOrderId;
    private Long warehouseLocationId;
    private Long returnedByUserId;
    private String reason;
    private List<ReturnedLineItem> returnedLineItems;

    @Getter
    @Setter
    @ToString
    public static class ReturnedLineItem {
        private Long productId;
        private BigDecimal quantityReturned;
        private BigDecimal unitCost;
    }
}