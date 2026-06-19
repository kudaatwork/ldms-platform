package projectlx.fleet.management.business.validator.api;

import projectlx.fleet.management.utils.requests.CreateFleetDriverSignupRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FleetDriverSignupRequestServiceValidator {

    ValidatorDto isCreateSignupRequestValid(CreateFleetDriverSignupRequest request, Locale locale);
}
