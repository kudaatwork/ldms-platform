package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class UsageChargeReportDto {
    private Long organizationId;
    private String billingMode;
    private Long tripId;
    private Long seasonId;
    private String periodFrom;
    private String periodTo;
    private Long totalChargeCents;
    private Long deductedChargeCents;
    private Long hypotheticalChargeCents;
    private List<UsageChargeBreakdownDto> breakdown;
    private List<UsageChargeRecordDto> records;
    private List<Long> dailyTotalsCents;
    private List<String> dailyLabels;
}
