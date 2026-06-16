package projectlx.fuel.expenses.business.auditable.api;

import projectlx.fuel.expenses.model.FuelTelemetryLog;

import java.util.Locale;

public interface FuelTelemetryLogServiceAuditable {

    FuelTelemetryLog create(FuelTelemetryLog fuelTelemetryLog, Locale locale, String username);
}
