package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UsageChargeRecordDto {
    private Long id;
    private String billingMode;
    private String actionCode;
    private String actionDisplayName;
    private Long chargeCents;
    private Boolean deducted;
    private Long tripId;
    private Long seasonId;
    private String referenceType;
    private Long referenceId;
    private String serviceName;
    private String createdAt;
}
