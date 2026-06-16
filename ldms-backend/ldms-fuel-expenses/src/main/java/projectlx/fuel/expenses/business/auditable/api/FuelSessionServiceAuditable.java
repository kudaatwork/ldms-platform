package projectlx.fuel.expenses.business.auditable.api;

import projectlx.fuel.expenses.model.FuelSession;

import java.util.Locale;

public interface FuelSessionServiceAuditable {

    FuelSession create(FuelSession fuelSession, Locale locale, String username);

    FuelSession update(FuelSession fuelSession, Locale locale, String username);
}
