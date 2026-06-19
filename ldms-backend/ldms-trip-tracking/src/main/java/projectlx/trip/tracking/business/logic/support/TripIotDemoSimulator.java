package projectlx.trip.tracking.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripServiceAuditable;
import projectlx.trip.tracking.clients.FleetManagementServiceClient;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.utils.config.IotIntegrationProperties;
import projectlx.trip.tracking.utils.dtos.FleetAssetSummaryDto;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
import projectlx.trip.tracking.utils.dtos.TripRouteWaypointDto;
import projectlx.trip.tracking.utils.enums.TripEventType;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.responses.FleetAssetFeignResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advances active demo simulations along the corridor route and emits IoT telemetry.
 * Real telematics units will replace this via MQTT once the hardware contract is live.
 */
@RequiredArgsConstructor
@Slf4j
public class TripIotDemoSimulator {

    private static final String SYSTEM_USER = "iot-demo-simulator";

    private final TripRoutePlanRepository tripRoutePlanRepository;
    private final TripRepository tripRepository;
    private final TripRoutePlannerSupport routePlannerSupport;
    private final TripTelemetryPublisher telemetryPublisher;
    private final TripEventServiceAuditable tripEventServiceAuditable;
    private final TripServiceAuditable tripServiceAuditable;
    private final TripTrailSupport tripTrailSupport;
    private final TripJourneyTimingSupport journeyTimingSupport;
    private final IotIntegrationProperties iotProperties;
    private final FleetManagementServiceClient fleetManagementServiceClient;

    private final Map<Long, BigDecimal> maxSpeedCache = new ConcurrentHashMap<>();

    public void startSimulation(Long tripId, String username) {
        TripRoutePlan plan = tripRoutePlanRepository.findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalStateException("Route plan not found for trip " + tripId));
        Trip trip = tripRepository.findByIdAndEntityStatusNotNoLock(tripId, EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalStateException("Trip not found: " + tripId));

        plan.setSimulationActive(true);
        plan.setSimulationPaused(false);
        plan.setCurrentSegmentIndex(0);
        plan.setSegmentProgressPct(BigDecimal.ZERO);
        plan.setOverallProgressPct(BigDecimal.ZERO);
        plan.setDistanceTravelledKm(BigDecimal.ZERO);
        plan.setCurrentLatitude(plan.getOriginLatitude());
        plan.setCurrentLongitude(plan.getOriginLongitude());
        plan.setCurrentSpeedKmh(iotProperties.getSimulationSpeedKmh().asBigDecimal());
        plan.setModifiedAt(LocalDateTime.now());
        plan.setModifiedBy(username);
        tripTrailSupport.clearTrail(plan);
        tripTrailSupport.appendTrailPoint(plan, plan.getOriginLatitude(), plan.getOriginLongitude(), BigDecimal.ZERO);
        journeyTimingSupport.resetTiming(plan, LocalDateTime.now());
        tripRoutePlanRepository.save(plan);
        cacheMaxSpeed(trip);
        publishCurrentSnapshot(trip, plan);
        log.info("IoT demo simulation started for trip {}", tripId);
    }

    public void stopSimulation(Long tripId, String username) {
        TripRoutePlan plan = tripRoutePlanRepository.findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (plan == null || !plan.isSimulationActive()) {
            return;
        }
        Trip trip = tripRepository.findByIdAndEntityStatusNotNoLock(tripId, EntityStatus.DELETED).orElse(null);
        haltMotion(plan, username, false);
        maxSpeedCache.remove(tripId);
        if (trip != null) {
            publishCurrentSnapshot(trip, plan);
        }
        log.info("IoT demo simulation stopped for trip {}", tripId);
    }

    public void pauseSimulation(Long tripId, String username) {
        TripRoutePlan plan = requireActivePlan(tripId);
        Trip trip = requireTrip(tripId);
        journeyTimingSupport.flush(plan, true, LocalDateTime.now());
        plan.setSimulationPaused(true);
        plan.setCurrentSpeedKmh(BigDecimal.ZERO);
        journeyTimingSupport.flush(plan, false, LocalDateTime.now());
        plan.setModifiedAt(LocalDateTime.now());
        plan.setModifiedBy(username);
        tripRoutePlanRepository.save(plan);
        recordDriverEvent(trip, TripEventType.DRIVER_BREAK, plan.getCurrentLatitude(), plan.getCurrentLongitude(),
                "Vehicle halted — driver break / manual stop");
        publishCurrentSnapshot(trip, plan);
        log.info("IoT demo simulation paused for trip {}", tripId);
    }

    public void resumeSimulation(Long tripId, String username) {
        TripRoutePlan plan = requireActivePlan(tripId);
        Trip trip = requireTrip(tripId);
        if (trip.getStatus() != TripStatus.IN_TRANSIT) {
            throw new IllegalStateException("Trip is not in transit");
        }
        plan.setSimulationPaused(false);
        BigDecimal speed = resolveConfiguredSpeed(trip);
        plan.setCurrentSpeedKmh(speed);
        journeyTimingSupport.flush(plan, false, LocalDateTime.now());
        journeyTimingSupport.flush(plan, true, LocalDateTime.now());
        plan.setModifiedAt(LocalDateTime.now());
        plan.setModifiedBy(username);
        tripRoutePlanRepository.save(plan);
        recordDriverEvent(trip, TripEventType.DRIVER_RESUMED, plan.getCurrentLatitude(), plan.getCurrentLongitude(),
                "Vehicle resumed corridor movement");
        publishCurrentSnapshot(trip, plan);
        log.info("IoT demo simulation resumed for trip {}", tripId);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${ldms.iot.simulation-tick-ms:2000}", initialDelay = 5000)
    public void tickActiveSimulations() {
        List<TripRoutePlan> activePlans = tripRoutePlanRepository.findBySimulationActiveTrueAndEntityStatusNot(EntityStatus.DELETED);
        for (TripRoutePlan plan : activePlans) {
            try {
                advance(plan);
            } catch (Exception ex) {
                log.warn("Simulation tick failed for trip {}: {}", plan.getTripId(), ex.getMessage());
            }
        }
    }

    private void advance(TripRoutePlan plan) {
        if (plan.isSimulationPaused()) {
            journeyTimingSupport.flush(plan, false, LocalDateTime.now());
            tripRoutePlanRepository.save(plan);
            return;
        }

        Trip trip = tripRepository.findByIdAndEntityStatusNotNoLock(plan.getTripId(), EntityStatus.DELETED).orElse(null);
        if (trip == null || trip.getStatus() != TripStatus.IN_TRANSIT) {
            stopSimulation(plan, SYSTEM_USER);
            return;
        }

        List<TripRouteWaypointDto> path = routePlannerSupport.buildFullPath(plan);
        if (path.size() < 2) {
            return;
        }

        int segmentIndex = Math.min(plan.getCurrentSegmentIndex(), path.size() - 2);
        TripRouteWaypointDto from = path.get(segmentIndex);
        TripRouteWaypointDto to = path.get(segmentIndex + 1);

        double segmentKm = routePlannerSupport.haversineKm(from, to);
        if (segmentKm <= 0.01) {
            segmentIndex++;
            plan.setCurrentSegmentIndex(segmentIndex);
            plan.setSegmentProgressPct(BigDecimal.ZERO);
            tripRoutePlanRepository.save(plan);
            return;
        }

        BigDecimal speed = resolveConfiguredSpeed(trip);
        double hoursPerTick = iotProperties.getSimulationTickSeconds() / 3600.0;
        double kmPerTick = speed.doubleValue() * hoursPerTick;
        BigDecimal kmDelta = BigDecimal.valueOf(kmPerTick).setScale(4, RoundingMode.HALF_UP);
        double progressInc = (kmPerTick / segmentKm) * 100.0;

        BigDecimal segmentProgress = plan.getSegmentProgressPct().add(BigDecimal.valueOf(progressInc));
        boolean segmentComplete = segmentProgress.compareTo(new BigDecimal("100")) >= 0;

        if (segmentComplete) {
            segmentIndex++;
            segmentProgress = BigDecimal.ZERO;
            plan.setCurrentSegmentIndex(segmentIndex);
            if (segmentIndex >= path.size() - 1) {
                arriveAtDestination(plan, trip, path.get(path.size() - 1));
                return;
            }
            from = path.get(segmentIndex);
            to = path.get(segmentIndex + 1);
        }

        BigDecimal t = segmentComplete ? BigDecimal.ZERO : segmentProgress.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal lat = interpolate(from.getLatitude(), to.getLatitude(), t);
        BigDecimal lng = interpolate(from.getLongitude(), to.getLongitude(), t);
        BigDecimal overall = computeOverallProgress(path, segmentIndex, segmentProgress);

        BigDecimal travelled = plan.getDistanceTravelledKm() != null ? plan.getDistanceTravelledKm() : BigDecimal.ZERO;
        plan.setDistanceTravelledKm(travelled.add(BigDecimal.valueOf(kmPerTick)).setScale(2, RoundingMode.HALF_UP));
        plan.setSegmentProgressPct(segmentProgress.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
        plan.setOverallProgressPct(overall);
        plan.setCurrentLatitude(lat);
        plan.setCurrentLongitude(lng);
        plan.setCurrentSpeedKmh(speed);
        plan.setCurrentHeadingDeg(routePlannerSupport.bearingDegrees(from, to));
        plan.setModifiedAt(LocalDateTime.now());
        plan.setModifiedBy(SYSTEM_USER);
        tripTrailSupport.appendTrailPoint(plan, lat, lng, speed);
        journeyTimingSupport.flush(plan, true, LocalDateTime.now());
        tripRoutePlanRepository.save(plan);

        TripLiveSnapshotDto snapshot = buildSnapshot(trip, plan, path, true);
        snapshot.setDistanceKmDelta(kmDelta);
        TripSimulationFuelSupport.applyToSnapshot(snapshot, plan.getDistanceTravelledKm());
        telemetryPublisher.publish(trip, snapshot);
    }

    private void arriveAtDestination(TripRoutePlan plan, Trip trip, TripRouteWaypointDto destination) {
        plan.setSimulationActive(false);
        plan.setSimulationPaused(false);
        plan.setOverallProgressPct(new BigDecimal("100"));
        plan.setCurrentLatitude(destination.getLatitude());
        plan.setCurrentLongitude(destination.getLongitude());
        plan.setCurrentSpeedKmh(BigDecimal.ZERO);
        plan.setModifiedAt(LocalDateTime.now());
        plan.setModifiedBy(SYSTEM_USER);
        tripTrailSupport.appendTrailPoint(plan, destination.getLatitude(), destination.getLongitude(), BigDecimal.ZERO);
        tripRoutePlanRepository.save(plan);
        maxSpeedCache.remove(plan.getTripId());

        if (trip.getStatus() == TripStatus.IN_TRANSIT) {
            LocalDateTime now = LocalDateTime.now();
            trip.setStatus(TripStatus.ARRIVED);
            trip.setArrivedAt(now);
            trip.setModifiedAt(now);
            trip.setModifiedBy(SYSTEM_USER);
            tripServiceAuditable.update(trip, Locale.ENGLISH, SYSTEM_USER);
            TripEvent event = new TripEvent();
            event.setTrip(trip);
            event.setEventType(TripEventType.ARRIVED);
            event.setEventTime(now);
            event.setLatitude(destination.getLatitude());
            event.setLongitude(destination.getLongitude());
            event.setNotes("Simulation reached destination — awaiting delivery confirmation");
            event.setEntityStatus(EntityStatus.ACTIVE);
            event.setCreatedAt(now);
            event.setCreatedBy(SYSTEM_USER);
            tripEventServiceAuditable.create(event, Locale.ENGLISH, SYSTEM_USER);
        }

        TripLiveSnapshotDto snapshot = telemetryPublisher.buildSnapshot(
                trip, destination.getLatitude(), destination.getLongitude(),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"), false, false);
        snapshot.setRouteWaypoints(routePlannerSupport.buildFullPath(plan));
        snapshot.setTrail(tripTrailSupport.parseTrail(plan));
        snapshot.setDistanceTravelledKm(plan.getDistanceTravelledKm());
        TripSimulationFuelSupport.applyToSnapshot(snapshot, plan.getDistanceTravelledKm());
        telemetryPublisher.publish(trip, snapshot);
        log.info("IoT demo simulation completed for trip {} at destination", trip.getId());
    }

    private void stopSimulation(TripRoutePlan plan, String username) {
        if (!plan.isSimulationActive()) {
            return;
        }
        haltMotion(plan, username, false);
        maxSpeedCache.remove(plan.getTripId());
    }

    private void haltMotion(TripRoutePlan plan, String username, boolean keepSimulationActive) {
        plan.setSimulationActive(keepSimulationActive);
        plan.setSimulationPaused(false);
        plan.setCurrentSpeedKmh(BigDecimal.ZERO);
        journeyTimingSupport.flush(plan, false, LocalDateTime.now());
        plan.setModifiedAt(LocalDateTime.now());
        plan.setModifiedBy(username);
        tripRoutePlanRepository.save(plan);
    }

    private TripRoutePlan requireActivePlan(Long tripId) {
        TripRoutePlan plan = tripRoutePlanRepository.findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalStateException("Route plan not found for trip " + tripId));
        if (!plan.isSimulationActive()) {
            throw new IllegalStateException("Simulation is not active for trip " + tripId);
        }
        return plan;
    }

    private Trip requireTrip(Long tripId) {
        return tripRepository.findByIdAndEntityStatusNotNoLock(tripId, EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalStateException("Trip not found: " + tripId));
    }

    private void publishCurrentSnapshot(Trip trip, TripRoutePlan plan) {
        List<TripRouteWaypointDto> path = routePlannerSupport.buildFullPath(plan);
        boolean moving = plan.isSimulationActive() && !plan.isSimulationPaused()
                && trip.getStatus() == TripStatus.IN_TRANSIT
                && plan.getCurrentSpeedKmh() != null
                && plan.getCurrentSpeedKmh().compareTo(BigDecimal.ZERO) > 0;
        TripLiveSnapshotDto snapshot = buildSnapshot(trip, plan, path, moving);
        telemetryPublisher.publish(trip, snapshot);
    }

    private TripLiveSnapshotDto buildSnapshot(Trip trip, TripRoutePlan plan, List<TripRouteWaypointDto> path, boolean moving) {
        TripLiveSnapshotDto snapshot = telemetryPublisher.buildSnapshot(
                trip,
                plan.getCurrentLatitude(),
                plan.getCurrentLongitude(),
                nullSafe(plan.getCurrentSpeedKmh()),
                plan.getCurrentHeadingDeg(),
                nullSafe(plan.getOverallProgressPct()),
                plan.isSimulationActive(),
                moving);
        snapshot.setSimulationPaused(plan.isSimulationPaused());
        snapshot.setRouteWaypoints(path);
        snapshot.setTrail(tripTrailSupport.parseTrail(plan));
        snapshot.setDistanceTravelledKm(plan.getDistanceTravelledKm());
        snapshot.setOnBreak(plan.isSimulationPaused() || trip.getStatus() == TripStatus.ROADSIDE_HOLD);
        snapshot.setFleetAssetId(trip.getFleetAssetId());
        journeyTimingSupport.flush(plan, moving, LocalDateTime.now());
        journeyTimingSupport.populateSnapshot(trip, plan, snapshot, moving);
        applyFleetContext(trip, snapshot);
        if (plan.isSimulationActive()) {
            TripSimulationFuelSupport.applyToSnapshot(snapshot, plan.getDistanceTravelledKm());
        }
        return snapshot;
    }

    private void applyFleetContext(Trip trip, TripLiveSnapshotDto snapshot) {
        if (trip.getFleetAssetId() == null) {
            return;
        }
        try {
            FleetAssetFeignResponse response = fleetManagementServiceClient.findFleetAssetById(
                    trip.getFleetAssetId(), Locale.ENGLISH);
            FleetAssetSummaryDto asset = response != null ? response.getFleetAssetDto() : null;
            if (asset != null) {
                snapshot.setVehicleRegistration(asset.getRegistration());
                snapshot.setMaxSpeedKmh(asset.getMaxSpeedKmh());
                if (asset.getMaxSpeedKmh() != null && snapshot.getSpeedKmh() != null) {
                    snapshot.setSpeedLimitExceeded(snapshot.getSpeedKmh().compareTo(asset.getMaxSpeedKmh()) > 0);
                }
                if (snapshot.getDriverName() == null) {
                    snapshot.setDriverName(asset.getDriverName());
                }
            }
        } catch (Exception ex) {
            log.debug("Fleet asset lookup skipped for trip {}: {}", trip.getId(), ex.getMessage());
        }
    }

    private BigDecimal resolveConfiguredSpeed(Trip trip) {
        BigDecimal configured = iotProperties.getSimulationSpeedKmh().asBigDecimal();
        BigDecimal maxSpeed = maxSpeedCache.computeIfAbsent(trip.getId(), id -> loadMaxSpeed(trip));
        if (maxSpeed != null && configured.compareTo(maxSpeed) > 0) {
            return maxSpeed;
        }
        return configured;
    }

    private void cacheMaxSpeed(Trip trip) {
        maxSpeedCache.put(trip.getId(), loadMaxSpeed(trip));
    }

    private BigDecimal loadMaxSpeed(Trip trip) {
        if (trip.getFleetAssetId() == null) {
            return null;
        }
        try {
            FleetAssetFeignResponse response = fleetManagementServiceClient.findFleetAssetById(
                    trip.getFleetAssetId(), Locale.ENGLISH);
            FleetAssetSummaryDto asset = response != null ? response.getFleetAssetDto() : null;
            return asset != null ? asset.getMaxSpeedKmh() : null;
        } catch (Exception ex) {
            log.debug("Max speed lookup skipped for trip {}: {}", trip.getId(), ex.getMessage());
            return null;
        }
    }

    private void recordDriverEvent(Trip trip, TripEventType type, BigDecimal lat, BigDecimal lng, String notes) {
        LocalDateTime now = LocalDateTime.now();
        TripEvent event = new TripEvent();
        event.setTrip(trip);
        event.setEventType(type);
        event.setEventTime(now);
        event.setLatitude(lat);
        event.setLongitude(lng);
        event.setNotes(notes);
        event.setEntityStatus(EntityStatus.ACTIVE);
        event.setCreatedAt(now);
        event.setCreatedBy(SYSTEM_USER);
        tripEventServiceAuditable.create(event, Locale.ENGLISH, SYSTEM_USER);
    }

    private BigDecimal computeOverallProgress(List<TripRouteWaypointDto> path, int segmentIndex, BigDecimal segmentProgress) {
        int totalSegments = path.size() - 1;
        if (totalSegments <= 0) {
            return BigDecimal.ZERO;
        }
        double completed = segmentIndex + segmentProgress.doubleValue() / 100.0;
        return BigDecimal.valueOf((completed / totalSegments) * 100.0).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal interpolate(BigDecimal from, BigDecimal to, BigDecimal t) {
        double f = from.doubleValue();
        double tt = to.doubleValue();
        double ratio = t.doubleValue();
        return BigDecimal.valueOf(f + (tt - f) * ratio).setScale(7, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
