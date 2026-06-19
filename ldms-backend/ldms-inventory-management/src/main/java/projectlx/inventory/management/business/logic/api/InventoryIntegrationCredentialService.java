package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.model.InventoryIntegrationCredential;
import projectlx.inventory.management.utils.requests.CreateInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.requests.EditInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.responses.InventoryIntegrationCredentialResponse;

import java.util.Locale;
import java.util.Optional;

public interface InventoryIntegrationCredentialService {

    InventoryIntegrationCredentialResponse create(
            CreateInventoryIntegrationCredentialRequest request, Locale locale, String username);

    InventoryIntegrationCredentialResponse update(
            EditInventoryIntegrationCredentialRequest request, Locale locale, String username);

    InventoryIntegrationCredentialResponse findById(Long id, Locale locale, String username);

    InventoryIntegrationCredentialResponse findAllByOrganization(
            Long organizationId, Locale locale, String username);

    InventoryIntegrationCredentialResponse delete(Long id, Locale locale, String username);

    /**
     * Resolve credential by api_key — used internally by the integration ingest endpoint.
     */
    Optional<InventoryIntegrationCredential> resolveByApiKey(String apiKey);
}
