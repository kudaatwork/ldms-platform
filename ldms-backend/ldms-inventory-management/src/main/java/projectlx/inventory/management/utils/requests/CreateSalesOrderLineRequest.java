package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class CreateSalesOrderLineRequest {

    private Long salesOrderId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private UnitOfMeasure unitOfMeasure;
    private Long createdByUserId;
}