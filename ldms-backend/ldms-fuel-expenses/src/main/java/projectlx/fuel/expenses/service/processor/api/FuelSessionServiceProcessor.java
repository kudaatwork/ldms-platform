package projectlx.fuel.expenses.service.processor.api;

import projectlx.fuel.expenses.utils.responses.FuelSessionResponse;

import java.util.Locale;
import java.util.Map;

public interface FuelSessionServiceProcessor {

    FuelSessionResponse getLiveByTripId(Long tripId, Locale locale, String username);

    void onLocationUpdated(Map<String, Object> payload);
}
