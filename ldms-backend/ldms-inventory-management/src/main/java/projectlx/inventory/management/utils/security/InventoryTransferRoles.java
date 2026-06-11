package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum InventoryTransferRoles {

    CREATE_INVENTORY_TRANSFER("CREATE_INVENTORY_TRANSFER", "Creates inventory transfer"),
    CANCEL_INVENTORY_TRANSFER("CANCEL_INVENTORY_TRANSFER", "Cancels inventory transfer before completion"),
    UPDATE_INVENTORY_TRANSFER("UPDATE_INVENTORY_TRANSFER", "Updates inventory transfer information"),
    VIEW_INVENTORY_TRANSFER_BY_ID("VIEW_INVENTORY_TRANSFER_BY_ID", "Views inventory transfer by id"),
    VIEW_ALL_INVENTORY_TRANSFERS_AS_A_LIST("VIEW_ALL_INVENTORY_TRANSFERS_AS_A_LIST", "Views all inventory transfers as a list"),
    VIEW_ALL_INVENTORY_TRANSFERS_BY_MULTIPLE_FILTERS("VIEW_ALL_INVENTORY_TRANSFERS_BY_MULTIPLE_FILTERS", "Views all inventory transfers by multiple filters"),
    EXPORT_INVENTORY_TRANSFERS("EXPORT_INVENTORY_TRANSFERS", "Exports inventory transfers"),
    IMPORT_INVENTORY_TRANSFERS("IMPORT_INVENTORY_TRANSFERS", "Imports inventory transfers"),
    APPROVE_INVENTORY_TRANSFER("APPROVE_INVENTORY_TRANSFER", "Approves inventory transfer"),
    REJECT_INVENTORY_TRANSFER("REJECT_INVENTORY_TRANSFER", "Rejects inventory transfer with a reason"),
    START_TRANSIT_INVENTORY_TRANSFER("START_TRANSIT_INVENTORY_TRANSFER", "Starts transit for inventory transfer"),
    COMPLETE_INVENTORY_TRANSFER("COMPLETE_INVENTORY_TRANSFER", "Completes inventory transfer");

    private final String roleName;
    private final String description;
}
