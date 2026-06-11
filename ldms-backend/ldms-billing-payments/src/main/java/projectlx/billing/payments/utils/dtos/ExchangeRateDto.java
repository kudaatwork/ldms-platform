package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.enums.ExchangeRateSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExchangeRateDto {
    private Long id;
    private String fromCurrencyCode;
    private String toCurrencyCode;
    private BigDecimal rate;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private ExchangeRateSource source;
    private Boolean current;
}
