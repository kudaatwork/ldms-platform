package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ConvertCurrencyRequest {
    private String fromCurrencyCode;
    private String toCurrencyCode;
    private BigDecimal amount;
    private Long countryId;
    private LocalDate transactionDate;
}
