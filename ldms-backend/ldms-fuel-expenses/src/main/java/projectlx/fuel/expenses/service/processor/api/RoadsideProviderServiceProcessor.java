package projectlx.fuel.expenses.service.processor.api;

import projectlx.fuel.expenses.utils.responses.RoadsideProviderResponse;

import java.util.Locale;

public interface RoadsideProviderServiceProcessor {

    RoadsideProviderResponse listAll(Locale locale);

    RoadsideProviderResponse listNearby(double latitude, double longitude, double radiusKm, Locale locale);
}
