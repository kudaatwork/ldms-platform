package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SalesOrderLineRoles {

    CREATE_SALES_ORDER_LINE("CREATE_SALES_ORDER_LINE", "Creates sales order line"),
    DELETE_SALES_ORDER_LINE("DELETE_SALES_ORDER_LINE", "Deletes sales order line"),
    UPDATE_SALES_ORDER_LINE("UPDATE_SALES_ORDER_LINE", "Updates sales order line information"),
    VIEW_SALES_ORDER_LINE_BY_ID("VIEW_SALES_ORDER_LINE_BY_ID", "Views sales order line by id"),
    VIEW_ALL_SALES_ORDER_LINES_AS_A_LIST("VIEW_ALL_SALES_ORDER_LINES_AS_A_LIST", "Views all sales order lines as a list"),
    VIEW_ALL_SALES_ORDER_LINES_BY_MULTIPLE_FILTERS("VIEW_ALL_SALES_ORDER_LINES_BY_MULTIPLE_FILTERS", "Views all sales order lines by multiple filters"),
    EXPORT_SALES_ORDER_LINES("EXPORT_SALES_ORDER_LINES", "Exports sales order lines"),
    IMPORT_SALES_ORDER_LINES("IMPORT_SALES_ORDER_LINES", "Imports sales order lines");

    private final String roleName;
    private final String description;
}
