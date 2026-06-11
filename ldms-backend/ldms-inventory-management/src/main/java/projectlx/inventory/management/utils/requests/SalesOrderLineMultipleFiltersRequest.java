package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class SalesOrderLineMultipleFiltersRequest extends MultipleFiltersRequest {

    private Long salesOrderId;
    private Long productId;
    private UnitOfMeasure unitOfMeasure;
    private EntityStatus entityStatus;
}