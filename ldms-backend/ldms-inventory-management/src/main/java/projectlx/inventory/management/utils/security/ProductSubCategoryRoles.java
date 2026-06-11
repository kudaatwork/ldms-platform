package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProductSubCategoryRoles {

    CREATE_PRODUCT_SUB_CATEGORY("CREATE_PRODUCT_SUB_CATEGORY", "Creates product sub category"),
    DELETE_PRODUCT_SUB_CATEGORY("DELETE_PRODUCT_SUB_CATEGORY", "Deletes product sub category"),
    UPDATE_PRODUCT_SUB_CATEGORY("UPDATE_PRODUCT_SUB_CATEGORY", "Updates product sub category information"),
    VIEW_PRODUCT_SUB_CATEGORY_BY_ID("VIEW_PRODUCT_SUB_CATEGORY_BY_ID", "Views product sub category by id"),
    VIEW_ALL_PRODUCT_SUB_CATEGORIES_AS_A_LIST("VIEW_ALL_PRODUCT_SUB_CATEGORIES_AS_A_LIST", "Views all product sub categories as a list"),
    VIEW_ALL_PRODUCT_SUB_CATEGORIES_BY_MULTIPLE_FILTERS("VIEW_ALL_PRODUCT_SUB_CATEGORIES_BY_MULTIPLE_FILTERS", "Views all product sub categories by multiple filters"),
    EXPORT_PRODUCT_SUB_CATEGORIES("EXPORT_PRODUCT_SUB_CATEGORIES", "Exports product sub categories"),
    IMPORT_PRODUCT_SUB_CATEGORIES("IMPORT_PRODUCT_SUB_CATEGORIES", "Imports product sub categories");

    private final String roleName;
    private final String description;
}
