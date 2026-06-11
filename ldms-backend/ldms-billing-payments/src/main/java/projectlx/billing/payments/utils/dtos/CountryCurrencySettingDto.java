package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CountryCurrencySettingDto {
    private Long id;
    private Long countryId;
    private String countryName;
    private String countryIsoAlpha2;
    private String baseCurrencyCode;
}
