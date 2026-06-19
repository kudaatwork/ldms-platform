package projectlx.inventory.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.inventory.management.business.logic.api.LogisticsRouteStopService;
import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.utils.responses.LogisticsRouteStopResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

/**
 * System (service-to-service) endpoint for logistics route stop queries.
 * No JWT required — intended for internal microservice calls (e.g. trip-tracking).
 */
@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/logistics-route-stop")
@Tag(name = "Logistics Route Stop System Resource", description = "Service-to-service route stop queries (no JWT)")
@RequiredArgsConstructor
public class LogisticsRouteStopSystemResource {

    private final LogisticsRouteStopService logisticsRouteStopService;
    private static final Logger logger = LoggerFactory.getLogger(LogisticsRouteStopSystemResource.class);

    @GetMapping("/find-by-context")
    @Operation(
            summary = "Find route stops by context",
            description = "Returns all active route stops for the given context type and context ID. " +
                    "No JWT required — for internal service-to-service use.")
    public LogisticsRouteStopResponse findByContext(
            @Parameter(description = "Context type (e.g. INVENTORY_TRANSFER, SALES_ORDER, PURCHASE_ORDER)")
            @RequestParam RouteStopContextType contextType,
            @Parameter(description = "ID of the owning context record")
            @RequestParam Long contextId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {

        logger.info("System route stop query: contextType={} contextId={}", contextType, contextId);
        return logisticsRouteStopService.findByContext(contextType, contextId, locale, "system");
    }
}
