package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class PlatformRevenueOrgRowDto {

    private Long organizationId;
    private String organizationName;
    private String billingMode;
    private long earnedCents;
    private long costsCents;
    private long netCents;
    private long walletBalanceCents;
    private long depositCents;
    private long actionChargeCents;
    private long subscriptionUsageCents;
    private long totalUsageCents;
    private long usageEventCount;
    private String accent;
    private List<UsageChargeBreakdownDto> usageBreakdown = new ArrayList<>();
}
