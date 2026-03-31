package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAddressRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface AddressServiceValidator {
    ValidatorDto isCreateAddressRequestValid(CreateAddressRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditAddressRequest request, Locale locale);
}