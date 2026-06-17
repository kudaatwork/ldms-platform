package projectlx.trip.tracking.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.trip.tracking.service.processor.api.TripLiveServiceProcessor;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-trip-tracking/v1/backoffice/trip-live")
@Tag(name = "Trip live (backoffice)", description = "Cross-tenant corridor tracking for LX administrators")
@RequiredArgsConstructor
public class TripLiveBackofficeResource {

    private final TripLiveServiceProcessor tripLiveServiceProcessor;

    @Auditable(action = "BACKOFFICE_TRIP_LIVE_SNAPSHOT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/snapshot/{tripId}")
    @Operation(summary = "Live GPS snapshot for a trip")
    public ResponseEntity<TripResponse> snapshot(
            @PathVariable Long tripId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        TripResponse response = tripLiveServiceProcessor.getLiveSnapshotBackoffice(tripId, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_TRIP_LIVE_BY_SHIPMENT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-shipment/{shipmentId}")
    @Operation(summary = "Live snapshot for the active trip on a shipment")
    public ResponseEntity<TripResponse> byShipment(
            @PathVariable Long shipmentId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        TripResponse response = tripLiveServiceProcessor.getLiveSnapshotByShipmentBackoffice(shipmentId, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
