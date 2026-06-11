package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum InventoryItemRoles {

    CREATE_INVENTORY_ITEM("CREATE_INVENTORY_ITEM", "Creates inventory item"),
    DELETE_INVENTORY_ITEM("DELETE_INVENTORY_ITEM", "Deletes inventory item"),
    UPDATE_INVENTORY_ITEM("UPDATE_INVENTORY_ITEM", "Updates inventory item information"),
    VIEW_INVENTORY_ITEM_BY_ID("VIEW_INVENTORY_ITEM_BY_ID", "Views inventory item by id"),
    VIEW_ALL_INVENTORY_ITEMS_AS_A_LIST("VIEW_ALL_INVENTORY_ITEMS_AS_A_LIST", "Views all inventory items as a list"),
    VIEW_ALL_INVENTORY_ITEMS_BY_MULTIPLE_FILTERS("VIEW_ALL_INVENTORY_ITEMS_BY_MULTIPLE_FILTERS", "Views all inventory items by multiple filters"),
    EXPORT_INVENTORY_ITEMS("EXPORT_INVENTORY_ITEMS", "Exports inventory items"),
    IMPORT_INVENTORY_ITEMS("IMPORT_INVENTORY_ITEMS", "Imports inventory items");

    private final String roleName;
    private final String description;
}
