package projectlx.fuel.expenses.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.fuel.expenses.utils.requests.RecordFuelTelemetryRequest;

import java.util.Locale;

public interface FuelTelemetryLogServiceValidator {

    ValidatorDto isRecordTelemetryRequestValid(RecordFuelTelemetryRequest request, Locale locale);

    ValidatorDto isFindByTripIdRequestValid(Long tripId, Locale locale);
}
