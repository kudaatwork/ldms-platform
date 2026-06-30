package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateProductCategoryRequest {

    // Basic info
    private String name;
    private String description;
    private Long supplierId;
}
