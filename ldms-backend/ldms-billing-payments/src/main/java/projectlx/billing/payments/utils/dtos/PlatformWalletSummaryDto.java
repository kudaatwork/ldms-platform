package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformWalletSummaryDto {
    private Long organizationId;
    private String organizationName;
    private Long balanceCents;
    private String currencyCode;
    private String billingMode;
    private Long lowBalanceThresholdCents;
    private Boolean lowBalance;
    private Long subscriptionPackageId;
    private String subscriptionPackageName;
}
