package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovePurchaseOrderStageRequest {
    private Long purchaseOrderId;
    private Long approvedByUserId;
    private String approvalNotes;
}
