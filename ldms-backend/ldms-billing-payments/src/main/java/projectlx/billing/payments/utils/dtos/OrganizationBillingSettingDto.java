package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrganizationBillingSettingDto {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String billingMode;
    private Long subscriptionPackageId;
    private String subscriptionPackageName;
    private String subscriptionStartedAt;
    private String subscriptionRenewsAt;
    private Long lowBalanceThresholdCents;
}
