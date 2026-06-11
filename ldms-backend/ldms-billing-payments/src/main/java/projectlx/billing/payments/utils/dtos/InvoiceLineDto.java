package projectlx.billing.payments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceLineDto {
    private Long id;
    private Integer lineNumber;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPriceTransaction;
    private BigDecimal lineTotalTransaction;
    private BigDecimal unitPriceBase;
    private BigDecimal lineTotalBase;
    private Long exchangeRateSnapshotId;
}
