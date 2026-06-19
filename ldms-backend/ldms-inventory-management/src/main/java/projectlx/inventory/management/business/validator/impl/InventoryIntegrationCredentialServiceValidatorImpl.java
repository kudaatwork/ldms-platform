package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.validator.api.InventoryIntegrationCredentialServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.requests.EditInventoryIntegrationCredentialRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class InventoryIntegrationCredentialServiceValidatorImpl
        implements InventoryIntegrationCredentialServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(
            InventoryIntegrationCredentialServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateCredentialRequestValid(
            CreateInventoryIntegrationCredentialRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateInventoryIntegrationCredentialRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_INVALID_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getOrganizationId() == null || request.getOrganizationId() <= 0) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_ORG_ID_REQUIRED.getCode(), locale));
        }

        if (request.getCredentialLabel() == null || request.getCredentialLabel().isBlank()) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_LABEL_REQUIRED.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isEditCredentialRequestValid(
            EditInventoryIntegrationCredentialRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditInventoryIntegrationCredentialRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_INTEGRATION_CREDENTIAL_UPDATE_INVALID.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getId() == null || request.getId() <= 0) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id <= 0) {
            logger.info("Validation failed: ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }
}
