package projectlx.co.zw.fleetmanagement.business.validation.api;

import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FleetDriverServiceValidator {
    ValidatorDto isCreateFleetDriverRequestValid(CreateFleetDriverRequest request, Locale locale);
    ValidatorDto isEditFleetDriverRequestValid(EditFleetDriverRequest request, Locale locale);
}
