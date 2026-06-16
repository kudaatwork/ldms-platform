package projectlx.trip.tracking.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class TripLiveSseRegistry {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByTrip = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long tripId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emittersByTrip.computeIfAbsent(tripId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(tripId, emitter));
        emitter.onTimeout(() -> remove(tripId, emitter));
        emitter.onError(ex -> remove(tripId, emitter));
        return emitter;
    }

    public void broadcast(Long tripId, TripLiveSnapshotDto snapshot) {
        List<SseEmitter> emitters = emittersByTrip.get(tripId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("location").data(snapshot));
            } catch (IOException ex) {
                remove(tripId, emitter);
            }
        }
    }

    private void remove(Long tripId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByTrip.get(tripId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByTrip.remove(tripId);
            }
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // already closed
        }
    }
}
