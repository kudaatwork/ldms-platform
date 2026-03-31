package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditProvinceRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface ProvinceServiceValidator {
    ValidatorDto isCreateProvinceRequestValid(CreateProvinceRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditProvinceRequest request, Locale locale);
}