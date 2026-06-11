package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformActionChargeDto {
    private Long id;
    private String actionCode;
    private String displayName;
    private String description;
    private Long chargeCents;
    private String category;
    private Boolean active;
}
