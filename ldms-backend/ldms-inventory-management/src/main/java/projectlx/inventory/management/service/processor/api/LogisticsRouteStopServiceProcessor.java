package projectlx.inventory.management.service.processor.api;

import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.utils.requests.ReplaceRouteStopsRequest;
import projectlx.inventory.management.utils.responses.LogisticsRouteStopResponse;

import java.util.Locale;

public interface LogisticsRouteStopServiceProcessor {

    LogisticsRouteStopResponse replaceRouteStops(ReplaceRouteStopsRequest request, Locale locale, String username);

    LogisticsRouteStopResponse findByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username);

    LogisticsRouteStopResponse findById(Long id, Locale locale, String username);

    LogisticsRouteStopResponse deleteByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username);
}
