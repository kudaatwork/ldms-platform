package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PurchaseOrderRoles {

    CREATE_PURCHASE_ORDER("CREATE_PURCHASE_ORDER", "Creates purchase order"),
    DELETE_PURCHASE_ORDER("DELETE_PURCHASE_ORDER", "Deletes purchase order"),
    UPDATE_PURCHASE_ORDER("UPDATE_PURCHASE_ORDER", "Updates purchase order information"),
    VIEW_PURCHASE_ORDER_BY_ID("VIEW_PURCHASE_ORDER_BY_ID", "Views purchase order by id"),
    VIEW_ALL_PURCHASE_ORDERS_AS_A_LIST("VIEW_ALL_PURCHASE_ORDERS_AS_A_LIST", "Views all purchase orders as a list"),
    VIEW_ALL_PURCHASE_ORDERS_BY_MULTIPLE_FILTERS("VIEW_ALL_PURCHASE_ORDERS_BY_MULTIPLE_FILTERS", "Views all purchase orders by multiple filters"),
    EXPORT_PURCHASE_ORDERS("EXPORT_PURCHASE_ORDERS", "Exports purchase orders"),
    RECEIVE_GOODS("RECEIVE_GOODS", "Receives goods against purchase order"),
    IMPORT_PURCHASE_ORDERS("IMPORT_PURCHASE_ORDERS", "Imports purchase orders");

    private final String roleName;
    private final String description;
}
