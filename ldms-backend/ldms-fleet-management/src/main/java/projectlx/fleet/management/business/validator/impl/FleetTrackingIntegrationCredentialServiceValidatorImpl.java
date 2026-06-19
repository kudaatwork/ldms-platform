package projectlx.fleet.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fleet.management.business.validator.api.FleetTrackingIntegrationCredentialServiceValidator;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.enums.TrackingIntegrationProvider;
import projectlx.fleet.management.utils.requests.CreateFleetTrackingIntegrationCredentialRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FleetTrackingIntegrationCredentialServiceValidatorImpl
        implements FleetTrackingIntegrationCredentialServiceValidator {

    private static final Logger logger =
            LoggerFactory.getLogger(FleetTrackingIntegrationCredentialServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateRequestValid(CreateFleetTrackingIntegrationCredentialRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: create tracking integration credential request is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getOrganizationId() == null || request.getOrganizationId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"organizationId"}, locale));
        }
        if (request.getCredentialLabel() == null || request.getCredentialLabel().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"credentialLabel"}, locale));
        }
        if (request.getIntegrationProvider() == null || request.getIntegrationProvider().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"integrationProvider"}, locale));
        } else {
            try {
                TrackingIntegrationProvider provider =
                        TrackingIntegrationProvider.valueOf(request.getIntegrationProvider());
                if (provider == TrackingIntegrationProvider.LDMS_MOBILE) {
                    errors.add(messageService.getMessage(
                            I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_PROVIDER_NOT_ALLOWED.getCode(),
                            new String[]{}, locale));
                }
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_TRACKING_INTEGRATION_PROVIDER_INVALID.getCode(), new String[]{}, locale));
            }
        }
        if (request.getFleetAssetId() == null || request.getFleetAssetId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"fleetAssetId"}, locale));
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>())
                : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (id == null || id < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"id"}, locale));
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>())
                : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isOrganizationIdValid(Long organizationId, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (organizationId == null || organizationId < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"organizationId"}, locale));
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>())
                : new ValidatorDto(false, null, errors);
    }
}
