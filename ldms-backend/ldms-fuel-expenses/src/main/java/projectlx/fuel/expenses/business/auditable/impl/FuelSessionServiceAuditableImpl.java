package projectlx.fuel.expenses.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fuel.expenses.business.auditable.api.FuelSessionServiceAuditable;
import projectlx.fuel.expenses.model.FuelSession;
import projectlx.fuel.expenses.repository.FuelSessionRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class FuelSessionServiceAuditableImpl implements FuelSessionServiceAuditable {

    private final FuelSessionRepository fuelSessionRepository;

    @Override
    public FuelSession create(FuelSession fuelSession, Locale locale, String username) {
        return fuelSessionRepository.save(fuelSession);
    }

    @Override
    public FuelSession update(FuelSession fuelSession, Locale locale, String username) {
        return fuelSessionRepository.save(fuelSession);
    }
}
