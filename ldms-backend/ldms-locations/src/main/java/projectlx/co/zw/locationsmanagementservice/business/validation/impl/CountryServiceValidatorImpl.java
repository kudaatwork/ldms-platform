package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.CountryServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CountryMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCountryRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.doesStringHaveDigit;
import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@RequiredArgsConstructor
public class CountryServiceValidatorImpl implements CountryServiceValidator {

    private final MessageService messageService;
    private Logger logger = LoggerFactory.getLogger(CountryServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateCountryRequestValid(CreateCountryRequest createCountryRequest, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (createCountryRequest == null) {
            logger.info("Validation failed: CreateCountryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createCountryRequest.getName() == null || createCountryRequest.getName().isEmpty()) {
            logger.info("Validation failed: country name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (createCountryRequest.getIsoAlpha2Code() == null || createCountryRequest.getIsoAlpha2Code().isEmpty()) {
            logger.info("Validation failed: country iso alpha 2 code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_ISO_ALPHA_2_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (createCountryRequest.getIsoAlpha3Code() == null || createCountryRequest.getIsoAlpha3Code().isEmpty()) {
            logger.info("Validation failed: Iso alpha 3 code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_ISO_ALPHA_3_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (createCountryRequest.getDialCode() == null || createCountryRequest.getDialCode().isEmpty()) {
            logger.info("Validation failed: Dial code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_DIAL_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (createCountryRequest.getTimezone() == null || createCountryRequest.getTimezone().isEmpty()) {
            logger.info("Validation failed: Timezone is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_TIMEZONE_MISSING.getCode(), new String[]{}, locale));
        }

        if (createCountryRequest.getCurrencyCode() == null || createCountryRequest.getCurrencyCode().isEmpty()) {
            logger.info("Validation failed: Currency code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_CURRENCY_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (doesStringHaveDigit(createCountryRequest.getName())) {
            logger.info("Validation failed: Country name contains digits");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_NAME_INVALID.getCode(), new String[]{}, locale));
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
    public ValidatorDto isRequestValidForEditing(EditCountryRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditCountryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: country name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getIsoAlpha2Code() == null || request.getIsoAlpha2Code().isEmpty()) {
            logger.info("Validation failed: country iso alpha 2 code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_ISO_ALPHA_2_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getIsoAlpha3Code() == null || request.getIsoAlpha3Code().isEmpty()) {
            logger.info("Validation failed: Iso alpha 3 code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_ISO_ALPHA_3_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getDialCode() == null || request.getDialCode().isEmpty()) {
            logger.info("Validation failed: Dial code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_DIAL_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getTimezone() == null || request.getTimezone().isEmpty()) {
            logger.info("Validation failed: Timezone is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_TIMEZONE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getCurrencyCode() == null || request.getCurrencyCode().isEmpty()) {
            logger.info("Validation failed: Currency code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_CURRENCY_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (doesStringHaveDigit(request.getName())) {
            logger.info("Validation failed: Country name contains digits");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_NAME_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(CountryMultipleFiltersRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CountryMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_COUNTRY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
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

        if (input == null || input.isEmpty()) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
