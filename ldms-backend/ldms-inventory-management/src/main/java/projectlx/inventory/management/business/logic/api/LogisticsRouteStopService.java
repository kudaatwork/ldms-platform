package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.utils.requests.ReplaceRouteStopsRequest;
import projectlx.inventory.management.utils.requests.RouteStopRequest;
import projectlx.inventory.management.utils.responses.LogisticsRouteStopResponse;

import java.util.List;
import java.util.Locale;

public interface LogisticsRouteStopService {

    /**
     * Atomically replace all route stops for a given context (transfer/PO/SO).
     * Soft-deletes existing stops then inserts the new list in stop_sequence order.
     */
    LogisticsRouteStopResponse replaceRouteStops(
            RouteStopContextType contextType,
            Long contextId,
            List<RouteStopRequest> stops,
            Long organizationId,
            Locale locale,
            String username);

    LogisticsRouteStopResponse replaceRouteStops(ReplaceRouteStopsRequest request, Locale locale, String username);

    LogisticsRouteStopResponse findByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username);

    LogisticsRouteStopResponse findById(Long id, Locale locale, String username);

    LogisticsRouteStopResponse deleteByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username);
}
