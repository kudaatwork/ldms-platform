package projectlx.billing.payments.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.billing.payments.business.validator.api.DriverExpenseReconciliationServiceValidator;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.requests.ApproveDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.RejectDriverExpenseRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class DriverExpenseReconciliationServiceValidatorImpl implements DriverExpenseReconciliationServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(DriverExpenseReconciliationServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isApproveDriverExpenseRequestValid(ApproveDriverExpenseRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: ApproveDriverExpenseRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getId() == null || request.getId() < 1) {
            logger.info("Validation failed: driver expense ID is null or less than 1");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isRejectDriverExpenseRequestValid(RejectDriverExpenseRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: RejectDriverExpenseRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getId() == null || request.getId() < 1) {
            logger.info("Validation failed: driver expense ID is null or less than 1 for rejection");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            logger.info("Validation failed: rejectionReason is blank");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"rejectionReason"}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }
}
