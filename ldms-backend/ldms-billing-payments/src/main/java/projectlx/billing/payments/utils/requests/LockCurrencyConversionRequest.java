package projectlx.billing.payments.utils.requests;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Locks a spot exchange rate at a transaction date and converts to an organisation's functional currency.
 */
@Getter
@Setter
public class LockCurrencyConversionRequest {
    private Long organizationId;
    private String transactionCurrencyCode;
    private BigDecimal amount;
    private LocalDate transactionDate;
}
