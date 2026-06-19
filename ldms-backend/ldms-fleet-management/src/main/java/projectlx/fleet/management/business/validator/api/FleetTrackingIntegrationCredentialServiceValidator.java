package projectlx.fleet.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.fleet.management.utils.requests.CreateFleetTrackingIntegrationCredentialRequest;

import java.util.Locale;

public interface FleetTrackingIntegrationCredentialServiceValidator {

    ValidatorDto isCreateRequestValid(CreateFleetTrackingIntegrationCredentialRequest request, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);

    ValidatorDto isOrganizationIdValid(Long organizationId, Locale locale);
}
