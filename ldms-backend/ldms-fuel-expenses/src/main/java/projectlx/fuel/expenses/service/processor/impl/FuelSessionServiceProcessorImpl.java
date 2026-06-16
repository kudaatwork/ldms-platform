package projectlx.fuel.expenses.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fuel.expenses.business.logic.api.FuelSessionService;
import projectlx.fuel.expenses.service.processor.api.FuelSessionServiceProcessor;
import projectlx.fuel.expenses.utils.responses.FuelSessionResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FuelSessionServiceProcessorImpl implements FuelSessionServiceProcessor {

    private final FuelSessionService fuelSessionService;

    @Override
    public FuelSessionResponse getLiveByTripId(Long tripId, Locale locale, String username) {
        log.info("Processing getLiveByTripId for tripId={} by user={}", tripId, username);
        return fuelSessionService.getLiveByTripId(tripId, locale, username);
    }
}
