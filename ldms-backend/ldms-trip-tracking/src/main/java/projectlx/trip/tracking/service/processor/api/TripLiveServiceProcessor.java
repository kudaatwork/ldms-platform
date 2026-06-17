package projectlx.trip.tracking.service.processor.api;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.util.Locale;

public interface TripLiveServiceProcessor {

    TripResponse getLiveSnapshot(Long tripId, Locale locale, String username);

    TripResponse startDemoSimulation(Long tripId, Locale locale, String username);

    TripResponse stopDemoSimulation(Long tripId, Locale locale, String username);

    TripResponse pauseDemoSimulation(Long tripId, Locale locale, String username);

    TripResponse resumeDemoSimulation(Long tripId, Locale locale, String username);

    SseEmitter subscribeLiveUpdates(Long tripId, Locale locale, String username);
}
