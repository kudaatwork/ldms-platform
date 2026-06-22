package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformRevenueChargeLineDto {

    private String id;
    private String label;
    private String category;
    private long amountCents;
    private Long organizationId;
    private String organizationName;
    private String occurredAt;
    private boolean deducted;
}
