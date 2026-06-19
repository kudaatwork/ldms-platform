package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.requests.EditInventoryIntegrationCredentialRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface InventoryIntegrationCredentialServiceValidator {
    ValidatorDto isCreateCredentialRequestValid(CreateInventoryIntegrationCredentialRequest request, Locale locale);
    ValidatorDto isEditCredentialRequestValid(EditInventoryIntegrationCredentialRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
}
