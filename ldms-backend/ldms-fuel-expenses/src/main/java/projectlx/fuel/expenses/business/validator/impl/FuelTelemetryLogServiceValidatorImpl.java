package projectlx.fuel.expenses.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fuel.expenses.business.validator.api.FuelTelemetryLogServiceValidator;
import projectlx.fuel.expenses.utils.enums.I18Code;
import projectlx.fuel.expenses.utils.requests.RecordFuelTelemetryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FuelTelemetryLogServiceValidatorImpl implements FuelTelemetryLogServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FuelTelemetryLogServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isRecordTelemetryRequestValid(RecordFuelTelemetryRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: RecordFuelTelemetryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_TELEMETRY_TRIP_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getTripId() == null || request.getTripId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_TELEMETRY_TRIP_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getSource() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_TELEMETRY_SOURCE_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getReadingType() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_TELEMETRY_READING_TYPE_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (!errors.isEmpty()) {
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isFindByTripIdRequestValid(Long tripId, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (tripId == null || tripId < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_TELEMETRY_TRIP_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, new ArrayList<>());
    }
}
