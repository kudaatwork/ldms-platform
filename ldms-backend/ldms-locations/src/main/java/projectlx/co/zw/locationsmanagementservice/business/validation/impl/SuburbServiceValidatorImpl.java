package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.SuburbServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditSuburbRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@Service
@RequiredArgsConstructor
public class SuburbServiceValidatorImpl implements SuburbServiceValidator {

    private final MessageService messageService;
    private Logger logger = LoggerFactory.getLogger(SuburbServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateSuburbRequestValid(CreateSuburbRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateSuburbRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SUBURB_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: Suburb name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SUBURB_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getCode() == null || request.getCode().isEmpty()) {
            logger.info("Validation failed: Suburb code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SUBURB_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getPostalCode() == null || request.getPostalCode().isEmpty()) {
            logger.info("Validation failed: Suburb postal code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SUBURB_POSTAL_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getDistrictId() == null) {
            logger.info("Validation failed: District ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SUBURB_DISTRICT_ID_MISSING.getCode(), new String[]{}, locale));
        }


        if (request.getAdministrativeLevelId() == null) {
            logger.info("Validation failed: Administrative level ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SUBURB_ADMINISTRATIVE_LEVEL_ID_MISSING.getCode(), new String[]{}, locale));
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
    public ValidatorDto isRequestValidForEditing(EditSuburbRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditSuburbRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: Suburb name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getCode() == null || request.getCode().isEmpty()) {
            logger.info("Validation failed: Suburb code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getPostalCode() == null || request.getPostalCode().isEmpty()) {
            logger.info("Validation failed: Suburb postal code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_POSTAL_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getDistrictId() == null) {
            logger.info("Validation failed: District ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_DISTRICT_ID_MISSING.getCode(), new String[]{}, locale));
        }


        if (request.getAdministrativeLevelId() == null) {
            logger.info("Validation failed: Administrative level ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SUBURB_ADMINISTRATIVE_LEVEL_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
