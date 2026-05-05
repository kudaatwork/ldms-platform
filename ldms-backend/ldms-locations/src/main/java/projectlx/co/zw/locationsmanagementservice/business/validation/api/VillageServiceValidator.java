package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.VillageMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface VillageServiceValidator {

    ValidatorDto isCreateVillageRequestValid(CreateVillageRequest request, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);

    ValidatorDto isRequestValidForEditing(EditVillageRequest request, Locale locale);

    ValidatorDto isRequestValidToRetrieveVillagesByMultipleFilters(VillageMultipleFiltersRequest request, Locale locale);
}
