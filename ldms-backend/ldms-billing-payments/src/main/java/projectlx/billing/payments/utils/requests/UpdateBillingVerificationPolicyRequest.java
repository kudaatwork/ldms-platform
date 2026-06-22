package projectlx.billing.payments.utils.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBillingVerificationPolicyRequest {

    @Min(1)
    @Max(3)
    private Integer defaultRequiredVerificationStages;
}
