package projectlx.fuel.expenses.business.logic.api;

import projectlx.fuel.expenses.utils.responses.RoadsideProviderResponse;

import java.util.Locale;

public interface RoadsideProviderService {

    RoadsideProviderResponse listAll(Locale locale);

    RoadsideProviderResponse listNearby(double latitude, double longitude, double radiusKm, Locale locale);
}
