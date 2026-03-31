package projectlx.user.management.service.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserAddressServiceValidator;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.EditAddressRequest;
import projectlx.user.management.service.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.service.utils.requests.CreateAddressRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAddressServiceValidatorImpl implements UserAddressServiceValidator {
    private static Logger logger = LoggerFactory.getLogger(UserAddressServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateAddressRequestValid(CreateAddressRequest createAddressRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createAddressRequest == null) {
            logger.info("Validation failed: CreateAddressRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ADDRESS_REQUEST_IS_NULL.getCode(), new String[]{}, 
                    locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createAddressRequest.getLine1() == null || createAddressRequest.getLine1().isEmpty() ||
                createAddressRequest.getPostalCode() == null || createAddressRequest.getPostalCode().isEmpty() ||
                createAddressRequest.getSuburbId() == null) {
            logger.info("Validation failed: One or more required address fields are missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ADDRESS_REQUIRED_FIELDS_MISSING.getCode(), 
                    new String[]{}, locale));
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

        if (id == null || id < 1L) {
            logger.info("Validation failed: ID is null or less than 1");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_ID_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditAddressRequest editAddressRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (editAddressRequest == null) {
            logger.info("Validation failed: EditAddressRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_USER_ADDRESS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (editAddressRequest.getId() == null || editAddressRequest.getId() <= 0L) {
            logger.info("Validation failed: Address ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (editAddressRequest.getLine1() == null || editAddressRequest.getLine1().isEmpty() ||
                editAddressRequest.getPostalCode() == null || editAddressRequest.getPostalCode().isEmpty() ||
                editAddressRequest.getSuburbId() == null) {
            logger.info("Validation failed: One or more required address fields are missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ADDRESS_REQUIRED_FIELDS_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveAddressesByMultipleFilters(AddressMultipleFiltersRequest addressMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (addressMultipleFiltersRequest == null) {
            logger.info("Validation failed: UsersAddressMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (addressMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isStringValid(String input, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_STRING_INVALID.getCode(), new String[]{},
                    locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
