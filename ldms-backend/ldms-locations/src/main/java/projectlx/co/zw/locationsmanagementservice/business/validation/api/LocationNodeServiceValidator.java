package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface LocationNodeServiceValidator {
    ValidatorDto isCreateValid(CreateLocationNodeRequest request, Locale locale);
    ValidatorDto isEditValid(EditLocationNodeRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isFilterValid(LocationNodeMultipleFiltersRequest request, Locale locale);
}
