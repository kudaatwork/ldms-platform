package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

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
    private Boolean walletFrozen;
    private Boolean platformAccessAllowed;
    private Long subscriptionPackageId;
    private String subscriptionPackageName;
    /** When the current paid subscription month started / lapses (auto-revert to pay-as-you-go). */
    private String subscriptionStartedAt;
    private String subscriptionRenewsAt;
    /** Monthly SMS / WhatsApp quota from subscription package (included_standard_credits). */
    private Integer smsIncludedMonthly;
    private Long smsUsedThisPeriod;
    private Integer smsRemainingThisPeriod;
    /** True when subscription SMS quota is used up and wallet cannot cover overage. */
    private Boolean smsQuotaExhausted;
    /** Per-attribute subscription quota meters (messaging, milestone, tracking-day credits). */
    private List<SubscriptionQuotaMeterDto> subscriptionQuotas = new ArrayList<>();
}
