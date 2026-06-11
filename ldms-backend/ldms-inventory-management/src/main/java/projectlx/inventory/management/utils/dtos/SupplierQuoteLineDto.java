package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierQuoteLineDto {
    private Long id;
    private Long purchaseRequisitionLineId;
    private Integer lineNumber;
    private Long productId;
    private BigDecimal quotedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private Integer leadTimeDays;
    private String notes;
}
