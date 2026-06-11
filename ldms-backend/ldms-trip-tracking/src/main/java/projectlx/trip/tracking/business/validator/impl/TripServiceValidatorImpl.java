package projectlx.trip.tracking.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.trip.tracking.business.validator.api.TripServiceValidator;
import projectlx.trip.tracking.utils.enums.I18Code;
import projectlx.trip.tracking.utils.enums.TripEventType;
import projectlx.trip.tracking.utils.requests.RecordLocationRequest;
import projectlx.trip.tracking.utils.requests.RecordTripEventRequest;
import projectlx.trip.tracking.utils.requests.StartTripRequest;
import projectlx.trip.tracking.utils.requests.TriggerArrivalRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class TripServiceValidatorImpl implements TripServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(TripServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isStartTripRequestValid(StartTripRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: StartTripRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getShipmentId() == null || request.getShipmentId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"shipmentId"}, locale));
        }
        if (request.getFleetDriverId() == null || request.getFleetDriverId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"fleetDriverId"}, locale));
        }
        if (request.getFleetAssetId() == null || request.getFleetAssetId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"fleetAssetId"}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isRecordTripEventRequestValid(RecordTripEventRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getTripId() == null || request.getTripId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"tripId"}, locale));
        }
        if (request.getEventType() == null || request.getEventType().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"eventType"}, locale));
        } else {
            try {
                TripEventType.valueOf(request.getEventType().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"eventType (invalid)"}, locale));
            }
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isRecordLocationRequestValid(RecordLocationRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getTripId() == null || request.getTripId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"tripId"}, locale));
        }
        if (request.getLatitude() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"latitude"}, locale));
        }
        if (request.getLongitude() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"longitude"}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isTriggerArrivalRequestValid(TriggerArrivalRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getTripId() == null || request.getTripId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"tripId"}, locale));
        }
        if (request.getDriverUserId() == null || request.getDriverUserId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"driverUserId"}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isVerifyDeliveryOtpRequestValid(VerifyDeliveryOtpRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getTripId() == null || request.getTripId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"tripId"}, locale));
        }
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"otp"}, locale));
        } else if (!request.getOtp().trim().matches("\\d{6}")) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"otp (must be 6 digits)"}, locale));
        }
        if (request.getReceiverUserId() == null || request.getReceiverUserId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"receiverUserId"}, locale));
        }

        return buildResult(errors);
    }

    private ValidatorDto buildResult(List<String> errors) {
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, new ArrayList<>());
        }
        return new ValidatorDto(false, null, errors);
    }
}
