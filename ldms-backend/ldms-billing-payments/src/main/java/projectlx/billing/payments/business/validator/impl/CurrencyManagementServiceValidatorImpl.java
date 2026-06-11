package projectlx.billing.payments.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.business.validator.api.CurrencyManagementServiceValidator;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.requests.ConvertCurrencyRequest;
import projectlx.billing.payments.utils.requests.CreateExchangeRateRequest;
import projectlx.billing.payments.utils.requests.LockCurrencyConversionRequest;
import projectlx.billing.payments.utils.requests.SaveCountryCurrencySettingRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationCurrencySettingRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class CurrencyManagementServiceValidatorImpl implements CurrencyManagementServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyManagementServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isSaveCountryCurrencySettingRequestValid(SaveCountryCurrencySettingRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: SaveCountryCurrencySettingRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getCountryId() == null || request.getCountryId() < 1) {
            logger.info("Validation failed: countryId is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"countryId"}, locale));
        }

        if (!StringUtils.hasText(request.getCountryName())) {
            logger.info("Validation failed: countryName is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"countryName"}, locale));
        }

        if (!StringUtils.hasText(request.getCountryIsoAlpha2())) {
            logger.info("Validation failed: countryIsoAlpha2 is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"countryIsoAlpha2"}, locale));
        }

        if (!StringUtils.hasText(request.getBaseCurrencyCode())) {
            logger.info("Validation failed: baseCurrencyCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"baseCurrencyCode"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isCreateExchangeRateRequestValid(CreateExchangeRateRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateExchangeRateRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!StringUtils.hasText(request.getFromCurrencyCode())) {
            logger.info("Validation failed: fromCurrencyCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"fromCurrencyCode"}, locale));
        }

        if (!StringUtils.hasText(request.getToCurrencyCode())) {
            logger.info("Validation failed: toCurrencyCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"toCurrencyCode"}, locale));
        }

        if (request.getRate() == null || request.getRate().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Validation failed: rate is null or not positive");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"rate"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isSaveOrganizationCurrencySettingRequestValid(SaveOrganizationCurrencySettingRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: SaveOrganizationCurrencySettingRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!StringUtils.hasText(request.getFunctionalCurrencyCode())) {
            logger.info("Validation failed: functionalCurrencyCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"functionalCurrencyCode"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isConvertCurrencyRequestValid(ConvertCurrencyRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: ConvertCurrencyRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getAmount() == null) {
            logger.info("Validation failed: amount is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"amount"}, locale));
        }

        if (!StringUtils.hasText(request.getFromCurrencyCode())) {
            logger.info("Validation failed: fromCurrencyCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"fromCurrencyCode"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isLockCurrencyConversionRequestValid(LockCurrencyConversionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: LockCurrencyConversionRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getOrganizationId() == null || request.getOrganizationId() < 1) {
            logger.info("Validation failed: organizationId is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"organizationId"}, locale));
        }

        if (request.getAmount() == null) {
            logger.info("Validation failed: amount is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"amount"}, locale));
        }

        if (!StringUtils.hasText(request.getTransactionCurrencyCode())) {
            logger.info("Validation failed: transactionCurrencyCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"transactionCurrencyCode"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }
}
