package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
public class FulfillSalesOrderRequest {

    private Long salesOrderId;
    private Long warehouseLocationId;
    private Long fulfilledByUserId;
    
    private List<FulfilledLineItem> fulfilledItems;

    // Idempotency key to make fulfillOrder idempotent on retries
    private String idempotencyKey;

    @Getter
    @Setter
    @ToString
    public static class FulfilledLineItem {
        private Long salesOrderLineId;
        private BigDecimal quantityFulfilled;
    }
}