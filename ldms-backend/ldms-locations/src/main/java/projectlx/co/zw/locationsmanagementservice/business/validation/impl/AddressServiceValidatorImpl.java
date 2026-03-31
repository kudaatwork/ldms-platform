package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.AddressServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditAddressRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@RequiredArgsConstructor
public class AddressServiceValidatorImpl implements AddressServiceValidator {

    private final MessageService messageService;
    private Logger logger = LoggerFactory.getLogger(AddressServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateAddressRequestValid(CreateAddressRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateAddressRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_ADDRESS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getLine1() == null || request.getLine1().isEmpty()) {
            logger.info("Validation failed: Address line1 is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_ADDRESS_LINE1_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getPostalCode() == null || request.getPostalCode().isEmpty()) {
            logger.info("Validation failed: Postal code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_ADDRESS_POSTAL_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getSuburbId() == null) {
            logger.info("Validation failed: Suburb ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_ADDRESS_SUBURB_ID_MISSING.getCode(), new String[]{}, locale));
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
    public ValidatorDto isRequestValidForEditing(EditAddressRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditAddressRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_ADDRESS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getLine1() == null || request.getLine1().isEmpty()) {
            logger.info("Validation failed: Address line1 is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_ADDRESS_LINE1_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getPostalCode() == null || request.getPostalCode().isEmpty()) {
            logger.info("Validation failed: Postal code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_ADDRESS_POSTAL_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getSuburbId() == null) {
            logger.info("Validation failed: Suburb ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_ADDRESS_SUBURB_ID_MISSING.getCode(), new String[]{}, locale));
        }


        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
