package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PurchaseOrderLineRoles {

    CREATE_PURCHASE_ORDER_LINE("CREATE_PURCHASE_ORDER_LINE", "Creates purchase order line"),
    DELETE_PURCHASE_ORDER_LINE("DELETE_PURCHASE_ORDER_LINE", "Deletes purchase order line"),
    UPDATE_PURCHASE_ORDER_LINE("UPDATE_PURCHASE_ORDER_LINE", "Updates purchase order line information"),
    VIEW_PURCHASE_ORDER_LINE_BY_ID("VIEW_PURCHASE_ORDER_LINE_BY_ID", "Views purchase order line by id"),
    VIEW_ALL_PURCHASE_ORDER_LINES_AS_A_LIST("VIEW_ALL_PURCHASE_ORDER_LINES_AS_A_LIST", "Views all purchase order lines as a list"),
    VIEW_ALL_PURCHASE_ORDER_LINES_BY_MULTIPLE_FILTERS("VIEW_ALL_PURCHASE_ORDER_LINES_BY_MULTIPLE_FILTERS", "Views all purchase order lines by multiple filters"),
    EXPORT_PURCHASE_ORDER_LINES("EXPORT_PURCHASE_ORDER_LINES", "Exports purchase order lines"),
    IMPORT_PURCHASE_ORDER_LINES("IMPORT_PURCHASE_ORDER_LINES", "Imports purchase order lines");

    private final String roleName;
    private final String description;
}
