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
public class EditProductSubCategoryRequest {

    private Long productSubCategoryId; // Identifier field

    // Editable fields
    private Long categoryId;
    private String name;
    private String description;
}
