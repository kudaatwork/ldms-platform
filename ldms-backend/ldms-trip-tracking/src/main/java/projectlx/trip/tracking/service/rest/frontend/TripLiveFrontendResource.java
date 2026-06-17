package projectlx.trip.tracking.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.trip.tracking.service.processor.api.TripLiveServiceProcessor;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-trip-tracking/v1/frontend/trip-live")
@Tag(name = "Trip Live IoT Resource", description = "Real-time corridor tracking, SSE streams, and IoT demo simulation")
@RequiredArgsConstructor
public class TripLiveFrontendResource {

    private final TripLiveServiceProcessor tripLiveServiceProcessor;

    @Auditable(action = "VIEW_TRIP_LIVE_SNAPSHOT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/snapshot/{tripId}")
    @Operation(summary = "Current live GPS snapshot and corridor route")
    public ResponseEntity<TripResponse> snapshot(
            @PathVariable Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripLiveServiceProcessor.getLiveSnapshot(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "START_TRIP_DEMO_SIMULATION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/demo-simulation/{tripId}/start")
    @Operation(summary = "Start IoT demo simulation",
            description = "Animates truck movement along the corridor route until destination. Replaced by MQTT telematics when hardware contract is live.")
    public ResponseEntity<TripResponse> startDemoSimulation(
            @PathVariable Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripLiveServiceProcessor.startDemoSimulation(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "STOP_TRIP_DEMO_SIMULATION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/demo-simulation/{tripId}/stop")
    @Operation(summary = "Stop IoT demo simulation", description = "Ends corridor simulation and freezes the truck at its current position.")
    public ResponseEntity<TripResponse> stopDemoSimulation(
            @PathVariable Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripLiveServiceProcessor.stopDemoSimulation(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "PAUSE_TRIP_DEMO_SIMULATION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/demo-simulation/{tripId}/pause")
    @Operation(summary = "Halt vehicle on corridor", description = "Pauses movement while keeping simulation active — mirrors a driver break or manual stop.")
    public ResponseEntity<TripResponse> pauseDemoSimulation(
            @PathVariable Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripLiveServiceProcessor.pauseDemoSimulation(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "RESUME_TRIP_DEMO_SIMULATION")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/demo-simulation/{tripId}/resume")
    @Operation(summary = "Resume corridor movement", description = "Resumes demo or telematics motion after a driver break.")
    public ResponseEntity<TripResponse> resumeDemoSimulation(
            @PathVariable Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripLiveServiceProcessor.resumeDemoSimulation(tripId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SUBSCRIBE_TRIP_LIVE_STREAM")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/stream/{tripId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE live location stream", description = "Server-sent events for map animation and fuel telemetry.")
    public SseEmitter stream(
            @PathVariable Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return tripLiveServiceProcessor.subscribeLiveUpdates(tripId, locale, username);
    }
}
