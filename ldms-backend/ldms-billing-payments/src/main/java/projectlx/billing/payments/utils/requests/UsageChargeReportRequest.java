package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UsageChargeReportRequest {
    private Long tripId;
    private Long seasonId;
    private String from;
    private String to;
}
