package projectlx.billing.payments.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.business.validator.api.PlatformWalletBillingServiceValidator;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.requests.CreateWalletDepositRequest;
import projectlx.billing.payments.utils.requests.RecordPlatformUsageChargeRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationBillingSettingRequest;
import projectlx.billing.payments.utils.requests.SavePlatformActionChargeRequest;
import projectlx.billing.payments.utils.requests.SaveSubscriptionPackageRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class PlatformWalletBillingServiceValidatorImpl implements PlatformWalletBillingServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(PlatformWalletBillingServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isSaveBillingSettingRequestValid(SaveOrganizationBillingSettingRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: SaveOrganizationBillingSettingRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!StringUtils.hasText(request.getBillingMode())) {
            logger.info("Validation failed: billingMode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"billingMode"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isCreateWalletDepositRequestValid(CreateWalletDepositRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateWalletDepositRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getAmountCents() == null || request.getAmountCents() < 1) {
            logger.info("Validation failed: amountCents is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"amountCents"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isSaveActionChargeRequestValid(SavePlatformActionChargeRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: SavePlatformActionChargeRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!StringUtils.hasText(request.getActionCode())) {
            logger.info("Validation failed: actionCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"actionCode"}, locale));
        }

        if (request.getChargeCents() == null) {
            logger.info("Validation failed: chargeCents is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"chargeCents"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isSaveSubscriptionPackageRequestValid(SaveSubscriptionPackageRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: SaveSubscriptionPackageRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!StringUtils.hasText(request.getCode())) {
            logger.info("Validation failed: code is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"code"}, locale));
        }

        if (!StringUtils.hasText(request.getName())) {
            logger.info("Validation failed: name is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"name"}, locale));
        }

        if (request.getMonthlyPriceCents() == null) {
            logger.info("Validation failed: monthlyPriceCents is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"monthlyPriceCents"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isRecordUsageChargeRequestValid(RecordPlatformUsageChargeRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: RecordPlatformUsageChargeRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getOrganizationId() == null || request.getOrganizationId() < 1) {
            logger.info("Validation failed: organizationId is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"organizationId"}, locale));
        }

        if (!StringUtils.hasText(request.getActionCode())) {
            logger.info("Validation failed: actionCode is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"actionCode"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }
}
