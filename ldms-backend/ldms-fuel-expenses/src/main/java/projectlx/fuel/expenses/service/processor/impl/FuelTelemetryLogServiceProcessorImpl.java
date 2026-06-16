package projectlx.fuel.expenses.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fuel.expenses.business.logic.api.FuelTelemetryLogService;
import projectlx.fuel.expenses.service.processor.api.FuelTelemetryLogServiceProcessor;
import projectlx.fuel.expenses.utils.requests.RecordFuelTelemetryRequest;
import projectlx.fuel.expenses.utils.responses.FuelTelemetryLogResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FuelTelemetryLogServiceProcessorImpl implements FuelTelemetryLogServiceProcessor {

    private final FuelTelemetryLogService fuelTelemetryLogService;

    @Override
    public FuelTelemetryLogResponse record(RecordFuelTelemetryRequest request, Locale locale, String username) {
        log.info("Processing record telemetry for tripId={} source={} by user={}",
                request.getTripId(), request.getSource(), username);
        return fuelTelemetryLogService.record(request, locale, username);
    }

    @Override
    public FuelTelemetryLogResponse findByTripId(Long tripId, int page, int size, Locale locale, String username) {
        log.info("Processing findByTripId telemetry for tripId={} by user={}", tripId, username);
        return fuelTelemetryLogService.findByTripId(tripId, page, size, locale, username);
    }
}
