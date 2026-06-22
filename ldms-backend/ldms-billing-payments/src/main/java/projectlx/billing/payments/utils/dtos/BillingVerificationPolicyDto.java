package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingVerificationPolicyDto {
    private Integer defaultRequiredVerificationStages;
    private Integer minAllowedStages;
    private Integer maxAllowedStages;
}
