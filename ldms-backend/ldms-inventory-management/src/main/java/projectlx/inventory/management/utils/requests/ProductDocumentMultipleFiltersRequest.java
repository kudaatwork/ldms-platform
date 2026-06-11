package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class ProductDocumentMultipleFiltersRequest extends MultipleFiltersRequest {

    private String name;
    private String description;
    private EntityStatus entityStatus;
}
