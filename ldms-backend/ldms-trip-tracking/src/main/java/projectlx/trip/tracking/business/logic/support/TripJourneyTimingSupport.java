package projectlx.trip.tracking.business.logic.support;

import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
import projectlx.trip.tracking.utils.enums.TripStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tracks corridor transit vs waiting seconds on the route plan and exposes journey timing on live snapshots.
 */
public class TripJourneyTimingSupport {

    public void resetTiming(TripRoutePlan plan, LocalDateTime now) {
        plan.setTransitSeconds(0L);
        plan.setWaitingSeconds(0L);
        plan.setTimingLastTickAt(now);
        plan.setTimingLastMoving(true);
    }

    public void flush(TripRoutePlan plan, boolean moving, LocalDateTime now) {
        if (plan.getTimingLastTickAt() == null) {
            plan.setTimingLastTickAt(now);
            plan.setTimingLastMoving(moving);
            return;
        }
        long seconds = Duration.between(plan.getTimingLastTickAt(), now).getSeconds();
        if (seconds > 0) {
            if (plan.isTimingLastMoving()) {
                plan.setTransitSeconds(plan.getTransitSeconds() + seconds);
            } else {
                plan.setWaitingSeconds(plan.getWaitingSeconds() + seconds);
            }
        }
        plan.setTimingLastTickAt(now);
        plan.setTimingLastMoving(moving);
    }

    public void applyToSnapshot(Trip trip, TripRoutePlan plan, TripLiveSnapshotDto snapshot, boolean moving) {
        populateSnapshot(trip, plan, snapshot, moving);
    }

    public void flushAndPopulate(Trip trip,
                                 TripRoutePlan plan,
                                 TripLiveSnapshotDto snapshot,
                                 boolean moving,
                                 LocalDateTime now) {
        if (plan != null) {
            flush(plan, moving, now);
        }
        populateSnapshot(trip, plan, snapshot, moving);
    }

    public void populateSnapshot(Trip trip, TripRoutePlan plan, TripLiveSnapshotDto snapshot, boolean moving) {
        if (trip == null || snapshot == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime started = trip.getStartedAt();
        snapshot.setJourneyStartedAt(started);

        long transit = plan != null ? plan.getTransitSeconds() : 0L;
        long waiting = plan != null ? plan.getWaitingSeconds() : 0L;
        long total = started != null ? Math.max(0L, Duration.between(started, now).getSeconds()) : 0L;
        long idle = Math.max(0L, total - transit - waiting);

        snapshot.setTotalElapsedSeconds(total);
        snapshot.setTransitSeconds(transit);
        snapshot.setWaitingSeconds(waiting);
        snapshot.setIdleSeconds(idle);
        snapshot.setJourneyPhase(resolvePhase(trip, plan, moving));
        snapshot.setEstimatedArrivalSeconds(estimateArrivalSeconds(snapshot, transit, moving));
    }

    private static String resolvePhase(Trip trip, TripRoutePlan plan, boolean moving) {
        if (trip.getStatus() == TripStatus.DELIVERED || trip.getStatus() == TripStatus.CANCELLED) {
            return "COMPLETED";
        }
        if (trip.getStatus() == TripStatus.ROADSIDE_HOLD) {
            return "WAITING";
        }
        if (plan != null && plan.isSimulationPaused()) {
            return "WAITING";
        }
        if (moving) {
            return "TRANSIT";
        }
        if (plan != null && plan.isSimulationActive()) {
            return "WAITING";
        }
        return "IDLE";
    }

    private static Long estimateArrivalSeconds(TripLiveSnapshotDto snapshot, long transitSeconds, boolean moving) {
        if (!moving || transitSeconds <= 0) {
            return null;
        }
        BigDecimal progress = snapshot.getOverallProgressPct();
        if (progress == null) {
            return null;
        }
        double pct = progress.doubleValue();
        if (pct <= 0 || pct >= 100) {
            return null;
        }
        long remainingTransit = Math.round((transitSeconds / pct) * (100.0 - pct));
        return remainingTransit > 0 ? remainingTransit : null;
    }
}
