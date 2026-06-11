package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Resolved currency context for an organisation — used by inventory, billing UI, and other modules.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationCurrencyContextDto {
    private Long organizationId;
    private String organizationName;
    /** Currency used for books, inventory unit prices, and default PO/requisition currency. */
    private String functionalCurrencyCode;
    private String functionalCurrencyName;
    private String functionalCurrencySymbol;
    private Integer functionalCurrencyDecimalPlaces;
    /** Platform default for the organisation's country (suggestion only until org saves their own). */
    private String countryDefaultCurrencyCode;
    private Long countryId;
    private String countryIsoAlpha2;
    private List<CurrencyDto> availableCurrencies;
    /** Current exchange rates involving the functional currency (for display and conversion). */
    private List<ExchangeRateDto> activeExchangeRates;
}
