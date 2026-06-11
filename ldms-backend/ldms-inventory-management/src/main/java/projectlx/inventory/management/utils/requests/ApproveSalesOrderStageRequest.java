package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveSalesOrderStageRequest {
    private Long salesOrderId;
    private Long approvedByUserId;
    private Long fulfillmentWarehouseId;
    private String approvalNotes;
}
