package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RejectPurchaseRequisitionRequest {

    private Long id;
    private Long rejectedByUserId;
    private String rejectionReason;
}
