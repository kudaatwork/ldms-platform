package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationCurrencySettingDto {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private Long countryId;
    private String countryIsoAlpha2;
    private String functionalCurrencyCode;
    private String countryDefaultCurrencyCode;
}
