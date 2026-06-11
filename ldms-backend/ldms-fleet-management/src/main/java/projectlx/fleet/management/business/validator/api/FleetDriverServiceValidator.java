package projectlx.fleet.management.business.validator.api;

import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FleetDriverServiceValidator {
    ValidatorDto isCreateFleetDriverRequestValid(CreateFleetDriverRequest request, Locale locale);
    ValidatorDto isEditFleetDriverRequestValid(EditFleetDriverRequest request, Locale locale);
}
