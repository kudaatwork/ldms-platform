package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.enums.ExchangeRateSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateExchangeRateRequest {
    private String fromCurrencyCode;
    private String toCurrencyCode;
    private BigDecimal rate;
    private LocalDateTime effectiveFrom;
    private ExchangeRateSource source;
}
