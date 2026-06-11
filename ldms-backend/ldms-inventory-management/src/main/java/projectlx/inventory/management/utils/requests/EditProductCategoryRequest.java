package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditProductCategoryRequest {

    private Long productCategoryId; // Identifier field

    // Editable fields
    private String name;
    private String description;
}
