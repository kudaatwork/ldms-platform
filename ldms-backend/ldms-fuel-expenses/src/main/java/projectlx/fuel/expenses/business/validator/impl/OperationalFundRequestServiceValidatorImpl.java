package projectlx.fuel.expenses.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fuel.expenses.business.validator.api.OperationalFundRequestServiceValidator;
import projectlx.fuel.expenses.utils.enums.FundRequestType;
import projectlx.fuel.expenses.utils.enums.I18Code;
import projectlx.fuel.expenses.utils.requests.ApproveFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CancelFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CreateFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.FundRequestFilterRequest;
import projectlx.fuel.expenses.utils.requests.RejectFundRequestRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class OperationalFundRequestServiceValidatorImpl implements OperationalFundRequestServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(OperationalFundRequestServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateFundRequestValid(CreateFundRequestRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateFundRequestRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_TRIP_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getTripId() == null || request.getTripId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_TRIP_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getFleetDriverId() == null || request.getFleetDriverId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_DRIVER_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getRequestType() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_TYPE_REQUIRED.getCode(),
                    new String[]{}, locale));
        } else {
            if (request.getRequestType() == FundRequestType.FUEL_TOP_UP) {
                if (request.getLitersRequested() == null) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_LITERS_REQUIRED.getCode(),
                            new String[]{}, locale));
                } else if (request.getLitersRequested().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_LITERS_INVALID.getCode(),
                            new String[]{}, locale));
                }
            }
            if (request.getRequestType() == FundRequestType.FUNDS) {
                if (request.getAmountRequested() == null) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_AMOUNT_REQUIRED.getCode(),
                            new String[]{}, locale));
                } else if (request.getAmountRequested().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_AMOUNT_INVALID.getCode(),
                            new String[]{}, locale));
                }
            }
        }

        if (!errors.isEmpty()) {
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isApproveFundRequestValid(ApproveFundRequestRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null || request.getRequestId() == null || request.getRequestId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!errors.isEmpty()) {
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isRejectFundRequestValid(RejectFundRequestRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null || request.getRequestId() == null || request.getRequestId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_REJECTION_REASON_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (!errors.isEmpty()) {
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isCancelFundRequestValid(CancelFundRequestRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null || request.getRequestId() == null || request.getRequestId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (!errors.isEmpty()) {
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isFindByIdRequestValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isFindByMultipleFiltersRequestValid(FundRequestFilterRequest request, Locale locale) {
        if (request == null) {
            List<String> errors = new ArrayList<>();
            errors.add(messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_TRIP_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }
}
