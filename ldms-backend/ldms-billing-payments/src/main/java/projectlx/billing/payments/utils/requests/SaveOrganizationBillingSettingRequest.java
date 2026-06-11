package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SaveOrganizationBillingSettingRequest {
    private String billingMode;
    private Long subscriptionPackageId;
    private Long lowBalanceThresholdCents;
}
