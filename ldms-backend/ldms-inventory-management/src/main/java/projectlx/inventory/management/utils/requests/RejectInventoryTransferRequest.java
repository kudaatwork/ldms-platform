package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RejectInventoryTransferRequest {
    private Long transferId;
    private Long rejectedByUserId;
    private String rejectionReason;
}
