package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.inventory.management.business.auditable.api.InventoryIntegrationCredentialServiceAuditable;
import projectlx.inventory.management.model.InventoryIntegrationCredential;
import projectlx.inventory.management.repository.InventoryIntegrationCredentialRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class InventoryIntegrationCredentialServiceAuditableImpl
        implements InventoryIntegrationCredentialServiceAuditable {

    private final InventoryIntegrationCredentialRepository credentialRepository;

    @Override
    public InventoryIntegrationCredential create(InventoryIntegrationCredential credential, Locale locale, String username) {
        return credentialRepository.save(credential);
    }

    @Override
    public InventoryIntegrationCredential update(InventoryIntegrationCredential credential, Locale locale, String username) {
        return credentialRepository.save(credential);
    }

    @Override
    public InventoryIntegrationCredential delete(InventoryIntegrationCredential credential, Locale locale) {
        return credentialRepository.save(credential);
    }
}
