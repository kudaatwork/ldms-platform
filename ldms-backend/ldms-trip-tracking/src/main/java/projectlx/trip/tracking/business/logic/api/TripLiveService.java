package projectlx.trip.tracking.business.logic.api;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.util.Locale;

public interface TripLiveService {

    TripResponse getLiveSnapshot(Long tripId, Locale locale, String username);

    TripResponse startDemoSimulation(Long tripId, Locale locale, String username);

    TripResponse stopDemoSimulation(Long tripId, Locale locale, String username);

    TripResponse pauseDemoSimulation(Long tripId, Locale locale, String username);

    TripResponse resumeDemoSimulation(Long tripId, Locale locale, String username);

    SseEmitter subscribeLiveUpdates(Long tripId, Locale locale, String username);

    TripLiveSnapshotDto buildSnapshotFromPlan(Long tripId);

    /** Cross-tenant live snapshot for LX admin portal (no organisation scope). */
    TripResponse getLiveSnapshotBackoffice(Long tripId, Locale locale);

    /** Resolves the active trip for a shipment and returns its live snapshot. */
    TripResponse getLiveSnapshotByShipmentBackoffice(Long shipmentId, Locale locale);
}
