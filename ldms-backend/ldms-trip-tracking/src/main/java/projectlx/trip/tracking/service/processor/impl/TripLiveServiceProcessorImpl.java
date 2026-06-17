package projectlx.trip.tracking.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import projectlx.trip.tracking.business.logic.api.TripLiveService;
import projectlx.trip.tracking.service.processor.api.TripLiveServiceProcessor;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class TripLiveServiceProcessorImpl implements TripLiveServiceProcessor {

    private final TripLiveService tripLiveService;

    @Override
    public TripResponse getLiveSnapshot(Long tripId, Locale locale, String username) {
        log.info("Processing live snapshot for trip {} by {}", tripId, username);
        return tripLiveService.getLiveSnapshot(tripId, locale, username);
    }

    @Override
    public TripResponse startDemoSimulation(Long tripId, Locale locale, String username) {
        log.info("Processing demo simulation start for trip {} by {}", tripId, username);
        return tripLiveService.startDemoSimulation(tripId, locale, username);
    }

    @Override
    public TripResponse stopDemoSimulation(Long tripId, Locale locale, String username) {
        log.info("Processing demo simulation stop for trip {} by {}", tripId, username);
        return tripLiveService.stopDemoSimulation(tripId, locale, username);
    }

    @Override
    public TripResponse pauseDemoSimulation(Long tripId, Locale locale, String username) {
        log.info("Processing demo simulation pause for trip {} by {}", tripId, username);
        return tripLiveService.pauseDemoSimulation(tripId, locale, username);
    }

    @Override
    public TripResponse resumeDemoSimulation(Long tripId, Locale locale, String username) {
        log.info("Processing demo simulation resume for trip {} by {}", tripId, username);
        return tripLiveService.resumeDemoSimulation(tripId, locale, username);
    }

    @Override
    public SseEmitter subscribeLiveUpdates(Long tripId, Locale locale, String username) {
        log.info("Opening SSE stream for trip {} by {}", tripId, username);
        return tripLiveService.subscribeLiveUpdates(tripId, locale, username);
    }
}
