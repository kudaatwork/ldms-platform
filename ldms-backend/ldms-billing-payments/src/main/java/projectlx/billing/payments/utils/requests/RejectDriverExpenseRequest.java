package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectDriverExpenseRequest {
    private Long id;
    private String rejectionReason;
}
