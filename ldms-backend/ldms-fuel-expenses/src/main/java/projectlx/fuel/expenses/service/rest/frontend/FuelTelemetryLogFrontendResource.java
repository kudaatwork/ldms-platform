package projectlx.fuel.expenses.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fuel.expenses.service.processor.api.FuelTelemetryLogServiceProcessor;
import projectlx.fuel.expenses.utils.requests.RecordFuelTelemetryRequest;
import projectlx.fuel.expenses.utils.responses.FuelTelemetryLogResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-fuel-expenses/v1/frontend/fuel-telemetry-log")
@Tag(name = "Fuel Telemetry Log Frontend Resource",
        description = "Record and retrieve fuel telemetry readings for a trip")
@RequiredArgsConstructor
public class FuelTelemetryLogFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(FuelTelemetryLogFrontendResource.class);

    private final FuelTelemetryLogServiceProcessor fuelTelemetryLogServiceProcessor;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/record")
    @Operation(summary = "Record a fuel telemetry reading",
            description = "Accepts a fuel level, consumption, dispense or top-up reading from the driver app, " +
                    "telematics device, or manual entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Telemetry reading recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FuelTelemetryLogResponse> record(
            @RequestBody RecordFuelTelemetryRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /fuel-telemetry-log/record tripId={} source={} by user={}",
                request.getTripId(), request.getSource(), username);

        FuelTelemetryLogResponse response = fuelTelemetryLogServiceProcessor.record(request, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-trip/{tripId}")
    @Operation(summary = "Find telemetry logs for a trip",
            description = "Returns a paginated list of telemetry readings for the given trip, " +
                    "ordered by recorded_at descending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telemetry readings returned"),
            @ApiResponse(responseCode = "400", description = "Invalid trip ID"),
            @ApiResponse(responseCode = "404", description = "No telemetry data found for the trip"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FuelTelemetryLogResponse> findByTrip(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            Locale locale) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("POST /fuel-telemetry-log/find-by-trip/{} page={} size={} by user={}", tripId, page, size,
                username);

        FuelTelemetryLogResponse response =
                fuelTelemetryLogServiceProcessor.findByTripId(tripId, page, size, locale, username);

        if (!response.isSuccess()) {
            int status = response.getStatusCode() > 0 ? response.getStatusCode() : 400;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
