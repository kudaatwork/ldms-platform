package projectlx.fuel.expenses.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fuel.expenses.business.logic.api.RoadsideProviderService;
import projectlx.fuel.expenses.service.processor.api.RoadsideProviderServiceProcessor;
import projectlx.fuel.expenses.utils.responses.RoadsideProviderResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class RoadsideProviderServiceProcessorImpl implements RoadsideProviderServiceProcessor {

    private final RoadsideProviderService roadsideProviderService;

    @Override
    public RoadsideProviderResponse listAll(Locale locale) {
        return roadsideProviderService.listAll(locale);
    }

    @Override
    public RoadsideProviderResponse listNearby(double latitude, double longitude, double radiusKm, Locale locale) {
        return roadsideProviderService.listNearby(latitude, longitude, radiusKm, locale);
    }
}
