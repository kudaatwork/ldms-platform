package projectlx.fuel.expenses.utils.requests;

import lombok.Data;

@Data
public class RejectFundRequestRequest {

    private Long requestId;
    private String rejectionReason;
}
