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
public class ProductMultipleFiltersRequest extends MultipleFiltersRequest {

    private String name;
    private String productCode;
    private UnitOfMeasure unitOfMeasure;
    private String manufacturer;
    private EntityStatus entityStatus;
    /** Owning supplier organisation id — used by customers browsing a linked supplier catalogue. */
    private Long supplierId;
}
