package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.LogisticsRouteStop;

import java.util.Locale;

public interface LogisticsRouteStopServiceAuditable {
    LogisticsRouteStop create(LogisticsRouteStop routeStop, Locale locale, String username);
    LogisticsRouteStop update(LogisticsRouteStop routeStop, Locale locale, String username);
    LogisticsRouteStop delete(LogisticsRouteStop routeStop, Locale locale);
}
