package projectlx.trip.tracking.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.trip.tracking.business.validator.api.TripDeliveryServiceValidator;
import projectlx.trip.tracking.utils.enums.I18Code;
import projectlx.trip.tracking.utils.requests.FinishCountingRequest;
import projectlx.trip.tracking.utils.requests.RecordReturnLinesRequest;
import projectlx.trip.tracking.utils.requests.SendDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.StartCountingRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
public class TripDeliveryServiceValidatorImpl implements TripDeliveryServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(TripDeliveryServiceValidatorImpl.class);
    private static final Set<String> VALID_ACTOR_ROLES = Set.of("DRIVER", "CUSTOMER", "RECEIVER");
    private static final Set<String> VALID_OTP_CHANNELS = Set.of("SMS", "WHATSAPP", "EMAIL");

    private final MessageService messageService;

    @Override
    public ValidatorDto isStartCountingRequestValid(StartCountingRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getActorRole() == null || request.getActorRole().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"actorRole"}, locale));
        } else if (!VALID_ACTOR_ROLES.contains(request.getActorRole().trim().toUpperCase())) {
            errors.add("actorRole must be one of: DRIVER, CUSTOMER, RECEIVER");
        }

        if (!errors.isEmpty()) {
            logger.info("StartCounting validation failed: {}", errors);
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isFinishCountingRequestValid(FinishCountingRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getActorRole() == null || request.getActorRole().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"actorRole"}, locale));
        } else if (!VALID_ACTOR_ROLES.contains(request.getActorRole().trim().toUpperCase())) {
            errors.add("actorRole must be one of: DRIVER, CUSTOMER, RECEIVER");
        }

        if (!errors.isEmpty()) {
            logger.info("FinishCounting validation failed: {}", errors);
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isSendDeliveryOtpRequestValid(SendDeliveryOtpRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getTripId() == null || request.getTripId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"tripId"}, locale));
        }
        if (request.getChannel() == null || request.getChannel().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"channel"}, locale));
        } else if (!VALID_OTP_CHANNELS.contains(request.getChannel().trim().toUpperCase())) {
            errors.add("channel must be one of: SMS, WHATSAPP, EMAIL");
        }
        if (request.getRecipientContact() == null || request.getRecipientContact().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"recipientContact"}, locale));
        }

        if (!errors.isEmpty()) {
            logger.info("SendDeliveryOtp validation failed: {}", errors);
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isRecordReturnLinesRequestValid(RecordReturnLinesRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getActorRole() == null || request.getActorRole().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"actorRole"}, locale));
        } else if (!VALID_ACTOR_ROLES.contains(request.getActorRole().trim().toUpperCase())) {
            errors.add("actorRole must be one of: DRIVER, CUSTOMER, RECEIVER");
        }

        if (request.getReturnLines() == null || request.getReturnLines().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"returnLines"}, locale));
        } else {
            for (int i = 0; i < request.getReturnLines().size(); i++) {
                var line = request.getReturnLines().get(i);
                if (line.getProductName() == null || line.getProductName().isBlank()) {
                    errors.add("returnLines[" + i + "].productName is required");
                }
                if (line.getQuantity() == null || line.getQuantity().signum() < 0) {
                    errors.add("returnLines[" + i + "].quantity must be zero or positive");
                }
            }
        }

        if (!errors.isEmpty()) {
            logger.info("RecordReturnLines validation failed: {}", errors);
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }
}
