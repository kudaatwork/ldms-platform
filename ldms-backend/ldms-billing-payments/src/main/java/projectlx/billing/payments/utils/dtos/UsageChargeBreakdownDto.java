package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UsageChargeBreakdownDto {
    private String actionCode;
    private String actionDisplayName;
    private Long totalChargeCents;
    private Long eventCount;
}
