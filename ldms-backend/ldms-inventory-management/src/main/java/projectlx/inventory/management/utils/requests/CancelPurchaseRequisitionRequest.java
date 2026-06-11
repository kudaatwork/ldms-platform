package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CancelPurchaseRequisitionRequest {

    private Long id;
    private Long cancelledByUserId;
    private String cancellationReason;
}
