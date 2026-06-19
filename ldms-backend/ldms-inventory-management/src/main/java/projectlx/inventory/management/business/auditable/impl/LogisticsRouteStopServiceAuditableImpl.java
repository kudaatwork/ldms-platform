package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.inventory.management.business.auditable.api.LogisticsRouteStopServiceAuditable;
import projectlx.inventory.management.model.LogisticsRouteStop;
import projectlx.inventory.management.repository.LogisticsRouteStopRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class LogisticsRouteStopServiceAuditableImpl implements LogisticsRouteStopServiceAuditable {

    private final LogisticsRouteStopRepository logisticsRouteStopRepository;

    @Override
    public LogisticsRouteStop create(LogisticsRouteStop routeStop, Locale locale, String username) {
        return logisticsRouteStopRepository.save(routeStop);
    }

    @Override
    public LogisticsRouteStop update(LogisticsRouteStop routeStop, Locale locale, String username) {
        return logisticsRouteStopRepository.save(routeStop);
    }

    @Override
    public LogisticsRouteStop delete(LogisticsRouteStop routeStop, Locale locale) {
        return logisticsRouteStopRepository.save(routeStop);
    }
}
