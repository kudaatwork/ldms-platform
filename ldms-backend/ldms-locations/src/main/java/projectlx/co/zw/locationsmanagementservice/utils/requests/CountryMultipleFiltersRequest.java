package projectlx.co.zw.locationsmanagementservice.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@ToString
public class CountryMultipleFiltersRequest extends MultipleFiltersRequest {
    private String name;
    private String isoAlpha2Code;
    private String isoAlpha3Code;
    private String dialCode;
    private String timezone;
    private String currencyCode;
    private EntityStatus entityStatus;
}