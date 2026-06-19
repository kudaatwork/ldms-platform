package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.LogisticsRouteStopService;
import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.service.processor.api.LogisticsRouteStopServiceProcessor;
import projectlx.inventory.management.utils.requests.ReplaceRouteStopsRequest;
import projectlx.inventory.management.utils.responses.LogisticsRouteStopResponse;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LogisticsRouteStopServiceProcessorImpl implements LogisticsRouteStopServiceProcessor {

    private final LogisticsRouteStopService logisticsRouteStopService;
    private static final Logger logger = LoggerFactory.getLogger(LogisticsRouteStopServiceProcessorImpl.class);

    @Override
    public LogisticsRouteStopResponse replaceRouteStops(
            ReplaceRouteStopsRequest request, Locale locale, String username) {

        logger.info("Incoming request to replace route stops [context={}/{}] for user: {}",
                request != null ? request.getContextType() : null,
                request != null ? request.getContextId() : null,
                username);

        LogisticsRouteStopResponse response =
                logisticsRouteStopService.replaceRouteStops(request, locale, username);

        logger.info("Outgoing response after replacing route stops: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public LogisticsRouteStopResponse findByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username) {

        logger.info("Incoming request to find route stops [context={}/{}] for user: {}",
                contextType, contextId, username);

        LogisticsRouteStopResponse response =
                logisticsRouteStopService.findByContext(contextType, contextId, locale, username);

        logger.info("Outgoing response for route stop context query: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public LogisticsRouteStopResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find route stop by ID: {} for user: {}", id, username);

        LogisticsRouteStopResponse response =
                logisticsRouteStopService.findById(id, locale, username);

        logger.info("Outgoing response for route stop findById: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public LogisticsRouteStopResponse deleteByContext(
            RouteStopContextType contextType, Long contextId, Locale locale, String username) {

        logger.info("Incoming request to delete route stops [context={}/{}] for user: {}",
                contextType, contextId, username);

        LogisticsRouteStopResponse response =
                logisticsRouteStopService.deleteByContext(contextType, contextId, locale, username);

        logger.info("Outgoing response after deleting route stops: success={}",
                response != null && response.isSuccess());
        return response;
    }
}
