package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectWalletDepositRequest {
    private Long id;
    private String rejectionReason;
}
