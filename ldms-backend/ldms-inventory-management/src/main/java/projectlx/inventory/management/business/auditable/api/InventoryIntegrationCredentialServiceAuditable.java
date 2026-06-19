package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.InventoryIntegrationCredential;

import java.util.Locale;

public interface InventoryIntegrationCredentialServiceAuditable {
    InventoryIntegrationCredential create(InventoryIntegrationCredential credential, Locale locale, String username);
    InventoryIntegrationCredential update(InventoryIntegrationCredential credential, Locale locale, String username);
    InventoryIntegrationCredential delete(InventoryIntegrationCredential credential, Locale locale);
}
