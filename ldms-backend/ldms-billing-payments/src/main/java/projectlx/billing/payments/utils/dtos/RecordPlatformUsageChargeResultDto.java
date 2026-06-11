package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RecordPlatformUsageChargeResultDto {
    private Boolean allowed;
    private Boolean deducted;
    private Long chargeCents;
    private Long balanceAfterCents;
    private String billingMode;
    private String message;
}
