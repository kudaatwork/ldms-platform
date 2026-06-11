package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.InventoryTransfer;

import java.util.Locale;

public interface InventoryTransferServiceAuditable {
    InventoryTransfer create(InventoryTransfer inventoryTransfer, Locale locale, String username);
    InventoryTransfer update(InventoryTransfer inventoryTransfer, Locale locale, String username);
    InventoryTransfer delete(InventoryTransfer inventoryTransfer, Locale locale);
}
