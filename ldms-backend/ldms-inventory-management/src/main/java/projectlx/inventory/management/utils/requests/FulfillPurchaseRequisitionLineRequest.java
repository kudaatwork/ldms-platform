package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FulfillmentMethod;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class FulfillPurchaseRequisitionLineRequest {

    private Long purchaseRequisitionId;
    private Long lineId;
    private FulfillmentMethod fulfillmentMethod;
    private BigDecimal quantity;

    // For stock fulfillment
    private Long warehouseLocationId;

    // For transfer fulfillment
    private Long sourceWarehouseId;
    private Long destinationWarehouseId;

    // For tracking
    private Long fulfilledByUserId;
    private String fulfillmentNotes;
}
