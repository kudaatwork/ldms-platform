package projectlx.inventory.management.service.processor.api;

import projectlx.inventory.management.utils.requests.CreateInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.requests.EditInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.responses.InventoryIntegrationCredentialResponse;

import java.util.Locale;

public interface InventoryIntegrationCredentialServiceProcessor {

    InventoryIntegrationCredentialResponse create(
            CreateInventoryIntegrationCredentialRequest request, Locale locale, String username);

    InventoryIntegrationCredentialResponse update(
            EditInventoryIntegrationCredentialRequest request, Locale locale, String username);

    InventoryIntegrationCredentialResponse findById(Long id, Locale locale, String username);

    InventoryIntegrationCredentialResponse findAllByOrganization(
            Long organizationId, Locale locale, String username);

    InventoryIntegrationCredentialResponse delete(Long id, Locale locale, String username);
}
