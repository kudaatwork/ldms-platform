package projectlx.billing.payments.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.billing.payments.utils.dtos.ConversionResultDto;
import projectlx.billing.payments.utils.dtos.CountryCurrencySettingDto;
import projectlx.billing.payments.utils.dtos.CurrencyDto;
import projectlx.billing.payments.utils.dtos.ExchangeRateDto;
import projectlx.billing.payments.utils.dtos.OrganizationCurrencyContextDto;
import projectlx.billing.payments.utils.dtos.OrganizationCurrencySettingDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyResponse extends CommonResponse {
    private CurrencyDto currencyDto;
    private List<CurrencyDto> currencyDtoList;
    private CountryCurrencySettingDto countryCurrencySettingDto;
    private List<CountryCurrencySettingDto> countryCurrencySettingDtoList;
    private ExchangeRateDto exchangeRateDto;
    private List<ExchangeRateDto> exchangeRateDtoList;
    private ConversionResultDto conversionResultDto;
    private List<ExchangeRateDto> activeExchangeRates;
    private OrganizationCurrencySettingDto organizationCurrencySettingDto;
    private OrganizationCurrencyContextDto organizationCurrencyContextDto;
}
