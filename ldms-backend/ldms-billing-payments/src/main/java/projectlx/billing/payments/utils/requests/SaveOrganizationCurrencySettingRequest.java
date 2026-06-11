package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveOrganizationCurrencySettingRequest {
    private String functionalCurrencyCode;
    private Long countryId;
    private String countryIsoAlpha2;
    private String organizationName;
}
