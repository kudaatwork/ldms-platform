package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.AddressMultipleFiltersRequest;

import java.util.Locale;

public interface UserAddressServiceValidator {
    ValidatorDto isCreateAddressRequestValid(CreateAddressRequest createAddressRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditAddressRequest editAddressRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveAddressesByMultipleFilters(AddressMultipleFiltersRequest
                                                                        addressMultipleFiltersRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);
}
