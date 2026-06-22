package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreditOrganizationWalletRequest {
    private Long organizationId;
    private String organizationName;
    private Long amountCents;
    private String currencyCode;
    private String notes;
    /** When true, ensures prepaid wallet billing mode is enabled for the organisation. */
    private Boolean enablePrepaidBilling;
}
