package projectlx.fuel.expenses.service.processor.api;

import projectlx.fuel.expenses.utils.requests.RecordFuelTelemetryRequest;
import projectlx.fuel.expenses.utils.responses.FuelTelemetryLogResponse;

import java.util.Locale;

public interface FuelTelemetryLogServiceProcessor {

    FuelTelemetryLogResponse record(RecordFuelTelemetryRequest request, Locale locale, String username);

    FuelTelemetryLogResponse findByTripId(Long tripId, int page, int size, Locale locale, String username);
}
