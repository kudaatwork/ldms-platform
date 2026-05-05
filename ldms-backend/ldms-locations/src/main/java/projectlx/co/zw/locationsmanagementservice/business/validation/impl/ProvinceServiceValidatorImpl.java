package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.ProvinceServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditProvinceRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@RequiredArgsConstructor
public class ProvinceServiceValidatorImpl implements ProvinceServiceValidator {

    private final MessageService messageService;
    private Logger logger = LoggerFactory.getLogger(ProvinceServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateProvinceRequestValid(CreateProvinceRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateProvinceRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PROVINCE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: Province name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PROVINCE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getCountryId() == null) {
            logger.info("Validation failed: Country ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PROVINCE_COUNTRY_ID_MISSING.getCode(), new String[]{}, locale));
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
    public ValidatorDto isRequestValidForEditing(EditProvinceRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditProvinceRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PROVINCE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: Province name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PROVINCE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getCode() == null || request.getCode().isEmpty()) {
            logger.info("Validation failed: Province code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PROVINCE_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getCountryId() == null) {
            logger.info("Validation failed: Country ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PROVINCE_COUNTRY_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getAdministrativeLevelId() == null) {
            logger.info("Validation failed: Administrative level ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PROVINCE_ADMINISTRATIVE_LEVEL_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
