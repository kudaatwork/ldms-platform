package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class PlatformRevenueReportDto {

    private long totalEarnedCents;
    private long subscriptionCents;
    private long actionChargesCents;
    private long walletDepositsCents;
    private List<String> monthLabels = new ArrayList<>();
    private List<Long> earnedSeries = new ArrayList<>();
    private List<Long> costSeries = new ArrayList<>();
    private List<PlatformRevenueOrgRowDto> byOrganization = new ArrayList<>();
    private List<PlatformRevenueCategoryRowDto> costBreakdown = new ArrayList<>();
    private List<PlatformRevenueChargeLineDto> recentCharges = new ArrayList<>();
}
