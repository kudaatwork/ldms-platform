package projectlx.trip.tracking.service.rest.frontend;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.trip.tracking.service.processor.api.TripServiceProcessor;
import projectlx.trip.tracking.utils.requests.RecordLocationRequest;
import projectlx.trip.tracking.utils.requests.RecordTripEventRequest;
import projectlx.trip.tracking.utils.requests.StartTripRequest;
import projectlx.trip.tracking.utils.requests.TriggerArrivalRequest;
import projectlx.trip.tracking.utils.requests.TripFilterRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-trip-tracking/v1/frontend/trip")
@Tag(name = "Trip Frontend Resource", description = "Trip tracking operations for suppliers and logistics users")
@RequiredArgsConstructor
public class TripFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(TripFrontendResource.class);

    private final TripServiceProcessor tripServiceProcessor;

    @Auditable(action = "START_TRIP")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/start")
    @Operation(summary = "Start a trip", description = "Validates shipment allocation, creates the trip record IN_TRANSIT, notifies inventory and shipment services.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trip started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or shipment not allocated"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> startTrip(
            @RequestBody final StartTripRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripServiceProcessor.startTrip(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "RECORD_TRIP_EVENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/record-event")
    @Operation(summary = "Record a trip event", description = "Appends a trip event (checkpoint, border crossing, note, etc.) to the trip timeline.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> recordEvent(
            @RequestBody final RecordTripEventRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripServiceProcessor.recordEvent(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "RECORD_TRIP_LOCATION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/record-location")
    @Operation(summary = "Record GPS location", description = "Convenience endpoint for GPS pings — records a CHECKPOINT event with coordinates.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Location recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> recordLocation(
            @RequestBody final RecordLocationRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripServiceProcessor.recordLocation(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "TRIGGER_TRIP_ARRIVAL")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/trigger-arrival")
    @Operation(summary = "Trigger arrival and OTP",
               description = "Sets trip ARRIVED → OTP_PENDING, generates 6-digit OTP (BCrypt-hashed, 30 min expiry), and sends delivery notification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arrival triggered and OTP sent"),
            @ApiResponse(responseCode = "400", description = "Trip not IN_TRANSIT or invalid request"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> triggerArrival(
            @RequestBody final TriggerArrivalRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripServiceProcessor.triggerArrival(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "VERIFY_DELIVERY_OTP")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/verify-delivery-otp")
    @Operation(summary = "Verify delivery OTP",
               description = "Verifies the 6-digit OTP, completes inventory GRV, marks trip DELIVERED, updates shipment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> verifyDeliveryOtp(
            @RequestBody final VerifyDeliveryOtpRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripServiceProcessor.verifyDeliveryOtp(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "VIEW_TRIP")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find trip by id", description = "Returns trip details including up to 10 most recent events.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trip found"),
            @ApiResponse(responseCode = "404", description = "Trip not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> findById(
            @PathVariable final Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripServiceProcessor.findById(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_TRIPS")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Search trips", description = "Paginated search by organisation, status, and free-text term for supplier dashboard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trips retrieved"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<TripResponse> findByMultipleFilters(
            @RequestBody final TripFilterRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripServiceProcessor.findByMultipleFilters(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "TRACK_TRIP")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/track/{id}")
    @Operation(summary = "Public trip tracking view",
               description = "Returns full event timeline and current status for the trip. Suitable for supplier/customer tracking portal.")
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
}
