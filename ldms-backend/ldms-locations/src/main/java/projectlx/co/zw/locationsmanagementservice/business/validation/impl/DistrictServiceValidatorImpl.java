package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.DistrictServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.DistrictMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditDistrictRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@RequiredArgsConstructor
public class DistrictServiceValidatorImpl implements DistrictServiceValidator {

    private final MessageService messageService;
    private Logger logger = LoggerFactory.getLogger(DistrictServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateDistrictRequestValid(CreateDistrictRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateDistrictRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_DISTRICT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: District name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_DISTRICT_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getProvinceId() == null) {
            logger.info("Validation failed: Province ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_DISTRICT_PROVINCE_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (isNullOrLessThanOne(id)) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditDistrictRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditDistrictRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_DISTRICT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: District name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_DISTRICT_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getCode() == null || request.getCode().isEmpty()) {
            logger.info("Validation failed: District code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_DISTRICT_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getProvinceId() == null) {
            logger.info("Validation failed: Province ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_DISTRICT_PROVINCE_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getAdministrativeLevelId() == null) {
            logger.info("Validation failed: Administrative level ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_DISTRICT_ADMINISTRATIVE_LEVEL_ID_MISSING.getCode(), new String[]{}, locale));
        }


        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveDistrictsByMultipleFilters(DistrictMultipleFiltersRequest request, Locale locale) {
        // Simple validation that always returns success
        return new ValidatorDto(true, null, new ArrayList<>());
    }
}
