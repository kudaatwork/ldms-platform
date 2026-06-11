package projectlx.inventory.management.clients.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LockCurrencyConversionRequest {
    private Long organizationId;
    private String transactionCurrencyCode;
    private BigDecimal amount;
    private LocalDate transactionDate;
}
