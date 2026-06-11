package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CompleteInventoryTransferRequest {
    private Long transferId;
    private Long updatedByUserId;
    private String idempotencyKey;
}
