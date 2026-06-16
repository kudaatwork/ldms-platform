package projectlx.fuel.expenses.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fuel.expenses.business.auditable.api.FuelTelemetryLogServiceAuditable;
import projectlx.fuel.expenses.model.FuelTelemetryLog;
import projectlx.fuel.expenses.repository.FuelTelemetryLogRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class FuelTelemetryLogServiceAuditableImpl implements FuelTelemetryLogServiceAuditable {

    private final FuelTelemetryLogRepository fuelTelemetryLogRepository;

    @Override
    public FuelTelemetryLog create(FuelTelemetryLog fuelTelemetryLog, Locale locale, String username) {
        return fuelTelemetryLogRepository.save(fuelTelemetryLog);
    }
}
