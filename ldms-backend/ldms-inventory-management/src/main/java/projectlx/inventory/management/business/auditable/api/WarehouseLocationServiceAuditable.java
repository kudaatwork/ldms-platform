package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.WarehouseLocation;

import java.util.Locale;

public interface WarehouseLocationServiceAuditable {
    WarehouseLocation create(WarehouseLocation warehouseLocation, Locale locale, String username);
    WarehouseLocation update(WarehouseLocation warehouseLocation, Locale locale, String username);
    WarehouseLocation delete(WarehouseLocation warehouseLocation, Locale locale);
}
