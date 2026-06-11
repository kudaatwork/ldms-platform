package projectlx.inventory.management.clients.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillingConversionResultDto {
    private String fromCurrencyCode;
    private String toCurrencyCode;
    private BigDecimal sourceAmount;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRateUsed;
    private Long exchangeRateSnapshotId;
}
