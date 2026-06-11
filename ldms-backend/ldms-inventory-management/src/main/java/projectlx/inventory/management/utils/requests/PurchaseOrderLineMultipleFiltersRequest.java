package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class PurchaseOrderLineMultipleFiltersRequest extends MultipleFiltersRequest {

    private Long purchaseOrderId;
    private Long productId;
    private UnitOfMeasure unitOfMeasure;
    private EntityStatus entityStatus;

    // New fields for filtering
    private BigDecimal quantity;
    private BigDecimal unitPrice;
}
