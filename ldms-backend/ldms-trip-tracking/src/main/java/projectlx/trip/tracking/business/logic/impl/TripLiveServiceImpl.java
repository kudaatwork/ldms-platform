package projectlx.trip.tracking.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.trip.tracking.business.logic.api.TripLiveService;
import projectlx.trip.tracking.business.logic.support.CallerOrganizationResolver;
import projectlx.trip.tracking.business.logic.support.TripIotDemoSimulator;
import projectlx.trip.tracking.business.logic.support.TripLiveSseRegistry;
import projectlx.trip.tracking.business.logic.support.TripRoutePlannerSupport;
import projectlx.trip.tracking.business.logic.support.TripTelemetryPublisher;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
import projectlx.trip.tracking.utils.enums.I18Code;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.math.BigDecimal;
import java.util.Locale;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TripLiveServiceImpl implements TripLiveService {

    private final TripRepository tripRepository;
    private final TripRoutePlanRepository tripRoutePlanRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final TripRoutePlannerSupport routePlannerSupport;
    private final TripIotDemoSimulator demoSimulator;
    private final TripLiveSseRegistry sseRegistry;
    private final TripTelemetryPublisher telemetryPublisher;
    private final MessageService messageService;

    @Override
    public TripResponse getLiveSnapshot(Long tripId, Locale locale, String username) {
        Trip trip = requireAccessibleTrip(tripId, username, locale);
        if (trip == null) {
            return error(404, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        TripLiveSnapshotDto snapshot = buildSnapshotFromPlan(tripId);
        TripResponse response = success(200, messageService.getMessage(
                I18Code.MESSAGE_TRIP_LIVE_SNAPSHOT_SUCCESS.getCode(), new String[]{}, locale));
        response.setLiveSnapshot(snapshot);
        return response;
    }

    @Override
    @Transactional
    public TripResponse startDemoSimulation(Long tripId, Locale locale, String username) {
        Trip trip = requireAccessibleTrip(tripId, username, locale);
        if (trip == null) {
            return error(404, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.IN_TRANSIT) {
            return error(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_IN_TRANSIT.getCode(), new String[]{}, locale));
        }
        routePlannerSupport.ensureRoutePlan(trip, username);
        demoSimulator.startSimulation(tripId, username);
        TripLiveSnapshotDto snapshot = buildSnapshotFromPlan(tripId);
        TripResponse response = success(200, messageService.getMessage(
                I18Code.MESSAGE_TRIP_DEMO_SIMULATION_STARTED.getCode(), new String[]{}, locale));
        response.setLiveSnapshot(snapshot);
        return response;
    }

    @Override
    public SseEmitter subscribeLiveUpdates(Long tripId, Locale locale, String username) {
        Trip trip = requireAccessibleTrip(tripId, username, locale);
        if (trip == null) {
            throw new IllegalArgumentException(messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        SseEmitter emitter = sseRegistry.subscribe(tripId);
        try {
            TripLiveSnapshotDto snapshot = buildSnapshotFromPlan(tripId);
            emitter.send(SseEmitter.event().name("location").data(snapshot));
        } catch (Exception ex) {
            log.debug("Unable to send initial SSE snapshot for trip {}: {}", tripId, ex.getMessage());
        }
        return emitter;
    }

    @Override
    public TripLiveSnapshotDto buildSnapshotFromPlan(Long tripId) {
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return null;
        }
        TripRoutePlan plan = tripRoutePlanRepository.findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (plan == null) {
            return telemetryPublisher.buildSnapshot(
                    trip, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);
        }
        TripLiveSnapshotDto snapshot = telemetryPublisher.buildSnapshot(
                trip,
                plan.getCurrentLatitude(),
                plan.getCurrentLongitude(),
                plan.getCurrentSpeedKmh(),
                plan.getCurrentHeadingDeg(),
                plan.getOverallProgressPct(),
                plan.isSimulationActive(),
                plan.isSimulationActive() && trip.getStatus() == TripStatus.IN_TRANSIT);
        snapshot.setRouteWaypoints(routePlannerSupport.buildFullPath(plan));
        return snapshot;
    }

    private Trip requireAccessibleTrip(Long tripId, String username, Locale locale) {
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return null;
        }
        Long orgId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (orgId != null && !orgId.equals(trip.getOrganizationId())) {
            log.warn("User {} denied access to trip {} outside organisation {}", username, tripId, orgId);
            return null;
        }
        return trip;
    }

    private TripResponse success(int statusCode, String message) {
        TripResponse response = new TripResponse();
        response.setSuccess(true);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }

    private TripResponse error(int statusCode, String message) {
        TripResponse response = new TripResponse();
        response.setSuccess(false);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }
}
