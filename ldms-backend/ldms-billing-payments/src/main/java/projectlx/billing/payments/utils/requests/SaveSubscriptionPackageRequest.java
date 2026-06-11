package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SaveSubscriptionPackageRequest {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long monthlyPriceCents;
    private String currencyCode;
    private Integer sortOrder;
    private Boolean featured;
    private Boolean active;
}
