package projectlx.inventory.management.utils.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.FulfillmentMethod;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
public class ApprovePurchaseRequisitionRequest {

    @JsonAlias("purchaseRequisitionId")
    private Long id;
    private Long approvedByUserId;
    @JsonAlias("approvalComments")
    private String approvalNotes;

    // Line-level adjustments during approval
    @JsonAlias("lines")
    private List<LineApproval> lineApprovals;

    @Getter
    @Setter
    @ToString
    public static class LineApproval {
        @JsonAlias("purchaseRequisitionLineId")
        private Long lineId;
        private BigDecimal approvedQuantity; // Can differ from requested
        private FulfillmentMethod fulfillmentMethod; // Can be set/changed during approval
        @JsonAlias("approvalNotes")
        private String quantityAdjustmentReason; // Required if approved != requested
    }
}
