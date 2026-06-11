package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveCountryCurrencySettingRequest {
    private Long countryId;
    private String countryName;
    private String countryIsoAlpha2;
    private String baseCurrencyCode;
}
