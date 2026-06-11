package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.InventoryItem;

import java.util.Locale;

public interface InventoryItemServiceAuditable {
    InventoryItem create(InventoryItem inventoryItem, Locale locale, String username);
    InventoryItem update(InventoryItem inventoryItem, Locale locale, String username);
    InventoryItem delete(InventoryItem inventoryItem, Locale locale);
}
