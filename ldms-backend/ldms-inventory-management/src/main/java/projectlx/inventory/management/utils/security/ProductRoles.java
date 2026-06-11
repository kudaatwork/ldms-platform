package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProductRoles {

    CREATE_PRODUCT("CREATE_PRODUCT", "Creates product"),
    DELETE_PRODUCT("DELETE_PRODUCT", "Deletes product"),
    UPDATE_PRODUCT("UPDATE_PRODUCT", "Updates product information"),
    VIEW_PRODUCT_BY_ID("VIEW_PRODUCT_BY_ID", "Views product by id"),
    VIEW_ALL_PRODUCTS_AS_A_LIST("VIEW_ALL_PRODUCTS_AS_A_LIST", "Views all products as a list"),
    VIEW_ALL_PRODUCTS_BY_MULTIPLE_FILTERS("VIEW_ALL_PRODUCTS_BY_MULTIPLE_FILTERS", "Views all products by multiple filters"),
    EXPORT_PRODUCTS("EXPORT_PRODUCTS", "Exports products"),
    IMPORT_PRODUCTS("IMPORT_PRODUCTS", "Imports products");

    private final String roleName;
    private final String description;
}
