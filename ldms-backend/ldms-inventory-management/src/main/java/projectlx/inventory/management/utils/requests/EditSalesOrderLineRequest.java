package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class EditSalesOrderLineRequest {

    private Long salesOrderLineId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private UnitOfMeasure unitOfMeasure;
    private Long updatedByUserId;
    private EntityStatus entityStatus;
}