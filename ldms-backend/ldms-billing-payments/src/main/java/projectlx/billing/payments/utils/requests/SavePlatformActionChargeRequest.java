package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SavePlatformActionChargeRequest {
    private Long id;
    private String actionCode;
    private String displayName;
    private String description;
    private Long chargeCents;
    private String category;
    private String billingTier;
    private Boolean active;
}
