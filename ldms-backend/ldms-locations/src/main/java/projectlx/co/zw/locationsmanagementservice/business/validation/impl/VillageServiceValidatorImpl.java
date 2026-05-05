package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.VillageServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditVillageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.VillageMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@RequiredArgsConstructor
public class VillageServiceValidatorImpl implements VillageServiceValidator {

    private final MessageService messageService;
    private final Logger logger = LoggerFactory.getLogger(VillageServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateVillageRequestValid(CreateVillageRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: CreateVillageRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_VILLAGE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getName() == null || request.getName().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_VILLAGE_NAME_MISSING.getCode(), new String[]{}, locale));
        }
        if (request.getCityId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_VILLAGE_CITY_ID_MISSING.getCode(), new String[]{}, locale));
        }
        if (request.getDistrictId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_VILLAGE_DISTRICT_ID_MISSING.getCode(), new String[]{}, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        if (isNullOrLessThanOne(id)) {
            return new ValidatorDto(false, null, List.of(
                    messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale)));
        }
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditVillageRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_VILLAGE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (isNullOrLessThanOne(request.getId())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }
        if (request.getName() == null || request.getName().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_VILLAGE_NAME_MISSING.getCode(), new String[]{}, locale));
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveVillagesByMultipleFilters(VillageMultipleFiltersRequest request, Locale locale) {
        return new ValidatorDto(true, null, new ArrayList<>());
    }
}
