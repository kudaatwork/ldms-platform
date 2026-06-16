package projectlx.fuel.expenses.business.logic.api;

import projectlx.fuel.expenses.utils.requests.RecordFuelTelemetryRequest;
import projectlx.fuel.expenses.utils.responses.FuelTelemetryLogResponse;

import java.math.BigDecimal;
import java.util.Locale;

public interface FuelTelemetryLogService {

    FuelTelemetryLogResponse record(RecordFuelTelemetryRequest request, Locale locale, String username);

    FuelTelemetryLogResponse findByTripId(Long tripId, int page, int size, Locale locale, String username);

    /**
     * Internal method called by FuelSessionServiceImpl after a location update.
     * Logs a SYSTEM / CONSUMPTION_DELTA entry for audit and efficiency analysis.
     */
    void logConsumptionDelta(Long tripId, Long organizationId, Long fleetAssetId, Long fuelSessionId,
                             BigDecimal distanceDeltaKm, BigDecimal consumedLiters,
                             BigDecimal fuelLevelPct, BigDecimal fuelRemainingLiters,
                             BigDecimal latitude, BigDecimal longitude);

    /**
     * Internal method called after a TOP_UP is applied to the fuel session.
     */
    void logTopUp(Long tripId, Long organizationId, Long fleetAssetId, Long fuelSessionId,
                  BigDecimal approvedLiters, BigDecimal fuelLevelPct, BigDecimal fuelRemainingLiters);
}
