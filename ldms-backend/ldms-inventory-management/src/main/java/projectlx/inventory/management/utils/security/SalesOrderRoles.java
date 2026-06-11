package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SalesOrderRoles {

    CREATE_SALES_ORDER("CREATE_SALES_ORDER", "Creates sales order"),
    DELETE_SALES_ORDER("DELETE_SALES_ORDER", "Deletes sales order"),
    UPDATE_SALES_ORDER("UPDATE_SALES_ORDER", "Updates sales order information"),
    VIEW_SALES_ORDER_BY_ID("VIEW_SALES_ORDER_BY_ID", "Views sales order by id"),
    VIEW_ALL_SALES_ORDERS_AS_A_LIST("VIEW_ALL_SALES_ORDERS_AS_A_LIST", "Views all sales orders as a list"),
    VIEW_ALL_SALES_ORDERS_BY_MULTIPLE_FILTERS("VIEW_ALL_SALES_ORDERS_BY_MULTIPLE_FILTERS", "Views all sales orders by multiple filters"),
    EXPORT_SALES_ORDERS("EXPORT_SALES_ORDERS", "Exports sales orders"),
    IMPORT_SALES_ORDERS("IMPORT_SALES_ORDERS", "Imports sales orders");

    private final String roleName;
    private final String description;
}
