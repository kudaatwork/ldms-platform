package projectlx.inventory.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.service.processor.api.LogisticsRouteStopServiceProcessor;
import projectlx.inventory.management.utils.requests.ReplaceRouteStopsRequest;
import projectlx.inventory.management.utils.responses.LogisticsRouteStopResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/frontend/logistics-route-stop")
@Tag(name = "Logistics Route Stop Frontend Resource", description = "Manage route stops for transfers, POs and SOs")
@RequiredArgsConstructor
public class LogisticsRouteStopFrontendResource {

    private final LogisticsRouteStopServiceProcessor logisticsRouteStopServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(LogisticsRouteStopFrontendResource.class);

    @Auditable(action = "REPLACE_ROUTE_STOPS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/replace")
    @Operation(summary = "Replace route stops for a context",
               description = "Atomically replaces all route stops for the given context (transfer/PO/SO).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Route stops replaced successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<LogisticsRouteStopResponse> replaceRouteStops(
            @Valid @RequestBody final ReplaceRouteStopsRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                logisticsRouteStopServiceProcessor.replaceRouteStops(request, locale, username));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-context")
    @Operation(summary = "Find route stops by context",
               description = "Returns all active route stops ordered by stop_sequence for a given context.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Route stops retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<LogisticsRouteStopResponse> findByContext(
            @RequestParam final RouteStopContextType contextType,
            @RequestParam final Long contextId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                logisticsRouteStopServiceProcessor.findByContext(contextType, contextId, locale, username));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find route stop by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Route stop retrieved"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<LogisticsRouteStopResponse> findById(
            @PathVariable final Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(logisticsRouteStopServiceProcessor.findById(id, locale, username));
    }

    @Auditable(action = "DELETE_ROUTE_STOPS_BY_CONTEXT")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete-by-context")
    @Operation(summary = "Soft-delete all route stops for a context")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Route stops deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<LogisticsRouteStopResponse> deleteByContext(
            @RequestParam final RouteStopContextType contextType,
            @RequestParam final Long contextId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                logisticsRouteStopServiceProcessor.deleteByContext(contextType, contextId, locale, username));
    }
}
