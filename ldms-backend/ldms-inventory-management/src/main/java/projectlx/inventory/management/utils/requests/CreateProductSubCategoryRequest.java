package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateProductSubCategoryRequest {

    // Relationships
    private Long categoryId;

    // Basic info
    private String name;
    private String description;
}
