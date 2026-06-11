package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcknowledgeSupplierQuoteRequest {
    private Long purchaseRequisitionId;
    private Long acknowledgedByUserId;
    private String notes;
}
