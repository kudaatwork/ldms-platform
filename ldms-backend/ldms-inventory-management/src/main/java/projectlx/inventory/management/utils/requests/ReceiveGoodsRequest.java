package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
public class ReceiveGoodsRequest {

    private Long purchaseOrderId;
    private Long warehouseLocationId;
    private Long receivedByUserId;
    private List<ReceivedLineItem> receivedItems;

    // NEW FIELD: This is needed to capture comments for the GRV
    private String notes;

    // Idempotency key to make receiveGoods idempotent on retries
    private String idempotencyKey;

    /**
     * Represents a single item being received against a purchase order line.
     */
    @Getter
    @Setter
    @ToString
    public static class ReceivedLineItem {
        private Long purchaseOrderLineId;
        private BigDecimal quantityReceived;
        private String reason; // Optional reason for a quantity discrepancy
    }
}
