package projectlx.trip.tracking.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.trip.tracking.service.processor.api.TripServiceProcessor;
import projectlx.trip.tracking.utils.requests.RecordTripEventRequest;
import projectlx.trip.tracking.utils.requests.TripFilterRequest;
import projectlx.trip.tracking.utils.responses.TripResponse;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-trip-tracking/v1/system/trip")
@Tag(name = "Trip System Resource", description = "System-level trip operations for internal service-to-service calls")
@RequiredArgsConstructor
public class TripSystemResource {

    private static final Logger logger = LoggerFactory.getLogger(TripSystemResource.class);

    private final TripServiceProcessor tripServiceProcessor;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find trip by id (system)", description = "Returns trip details — for internal service-to-service consumption.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trip found"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> findById(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        TripResponse response = tripServiceProcessor.findById(id, locale, "system");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/track/{id}")
    @Operation(summary = "Track trip (system)", description = "Returns full event timeline — for internal service-to-service consumption.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trip tracking data retrieved"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> track(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        TripResponse response = tripServiceProcessor.track(id, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Search trips (system)", description = "Paginated trip search for system consumers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trips retrieved"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> findByMultipleFilters(
            @RequestBody final TripFilterRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        TripResponse response = tripServiceProcessor.findByMultipleFilters(request, locale, "system");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/record-event")
    @Operation(summary = "Record trip event (system)",
            description = "Records a trip event on behalf of another service (fuel-expenses, roadside). " +
                    "Supported event types: ARRIVED_AT_BORDER, BORDER_CLEARED, ROADSIDE_FUEL_STOP, " +
                    "ROADSIDE_MECHANIC_STOP, ROADSIDE_RESUMED, CHECKPOINT, NOTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trip event recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid request or trip not found"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> recordSystemEvent(
            @RequestBody final RecordTripEventRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("POST /system/trip/record-event tripId={} eventType={}", request.getTripId(), request.getEventType());
        TripResponse response = tripServiceProcessor.recordSystemEvent(request, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
