package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProductCategoryRoles {

    CREATE_PRODUCT_CATEGORY("CREATE_PRODUCT_CATEGORY", "Creates product category"),
    DELETE_PRODUCT_CATEGORY("DELETE_PRODUCT_CATEGORY", "Deletes product category"),
    UPDATE_PRODUCT_CATEGORY("UPDATE_PRODUCT_CATEGORY", "Updates product category information"),
    VIEW_PRODUCT_CATEGORY_BY_ID("VIEW_PRODUCT_CATEGORY_BY_ID", "Views product category by id"),
    VIEW_ALL_PRODUCT_CATEGORIES_AS_A_LIST("VIEW_ALL_PRODUCT_CATEGORIES_AS_A_LIST", "Views all product categories as a list"),
    VIEW_ALL_PRODUCT_CATEGORIES_BY_MULTIPLE_FILTERS("VIEW_ALL_PRODUCT_CATEGORIES_BY_MULTIPLE_FILTERS", "Views all product categories by multiple filters"),
    EXPORT_PRODUCT_CATEGORIES("EXPORT_PRODUCT_CATEGORIES", "Exports product categories"),
    IMPORT_PRODUCT_CATEGORIES("IMPORT_PRODUCT_CATEGORIES", "Imports product categories");

    private final String roleName;
    private final String description;
}
