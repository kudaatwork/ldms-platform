package projectlx.trip.tracking.business.logic.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.business.logic.api.TripTelemetryIngestService;
import projectlx.trip.tracking.business.logic.support.TripRoutePlannerSupport;
import projectlx.trip.tracking.business.logic.support.TripTelemetryPublisher;
import projectlx.trip.tracking.clients.FleetManagementServiceClient;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
import projectlx.trip.tracking.utils.dtos.TripRouteWaypointDto;
import projectlx.trip.tracking.utils.enums.TripEventType;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.requests.IngestTelemetryRequest;
import projectlx.trip.tracking.utils.responses.FleetTrackingDeviceFeignResponse;
import projectlx.trip.tracking.utils.responses.IngestTelemetryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class TripTelemetryIngestServiceImpl implements TripTelemetryIngestService {

    private static final String INGEST_ACTOR = "telemetry-ingest";

    private final FleetManagementServiceClient fleetManagementServiceClient;
    private final TripRepository tripRepository;
    private final TripRoutePlannerSupport routePlannerSupport;
    private final TripTelemetryPublisher telemetryPublisher;
    private final TripEventServiceAuditable tripEventServiceAuditable;
    private final TripRoutePlanRepository tripRoutePlanRepository;

    // ================================================================
    // REST / HTTP INGEST — authenticated by ingest key
    // ================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public IngestTelemetryResponse ingest(IngestTelemetryRequest request, Locale locale) {

        // STEP 1 — Resolve tracking device via ingest key
        if (request == null || request.getIngestKey() == null || request.getIngestKey().isBlank()) {
            return errorResponse(400, "ingestKey is required");
        }

        FleetTrackingDeviceFeignResponse device;
        try {
            device = fleetManagementServiceClient.resolveTrackingDeviceByIngestKey(
                    request.getIngestKey(), locale);
        } catch (FeignException.NotFound ex) {
            log.warn("Telemetry rejected — ingestKey not found");
            return errorResponse(401, "Invalid ingest key");
        } catch (Exception ex) {
            log.error("Feign error resolving tracking device by ingestKey: {}", ex.getMessage());
            return errorResponse(503, "Tracking device service unavailable");
        }

        if (device == null) {
            return errorResponse(401, "Invalid ingest key");
        }

        // STEP 2 — Validate device is ACTIVE and tracks GPS
        if (!"ACTIVE".equals(device.getInstallStatus())) {
            return errorResponse(400, "Device is not ACTIVE");
        }
        if (!device.isTracksGps()) {
            return errorResponse(400, "Device does not track GPS");
        }

        // STEP 3 — Find IN_TRANSIT trip for this asset
        Optional<Trip> tripOpt = tripRepository
                .findFirstByFleetAssetIdAndStatusAndEntityStatusNotOrderByStartedAtDesc(
                        device.getFleetAssetId(), TripStatus.IN_TRANSIT, EntityStatus.DELETED);

        if (tripOpt.isEmpty()) {
            // No active trip — still mark telemetry received on device
            markDeviceTelemetry(device.getId());
            IngestTelemetryResponse response = new IngestTelemetryResponse();
            response.setStatusCode(202);
            response.setMessage("Telemetry stored; no active trip");
            return response;
        }

        Trip trip = tripOpt.get();
        processLiveTelemetry(trip,
                request.getLatitude(), request.getLongitude(),
                request.getSpeedKmh(), request.getHeadingDeg());

        markDeviceTelemetry(device.getId());

        IngestTelemetryResponse response = new IngestTelemetryResponse();
        response.setStatusCode(200);
        response.setMessage("Telemetry ingested");
        response.setTripId(trip.getId());
        response.setTripNumber(trip.getTripNumber());
        return response;
    }

    // ================================================================
    // MQTT INGEST PATH — org/asset from topic parsing
    // ================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void ingestFromAsset(Long organizationId, Long fleetAssetId,
                                BigDecimal latitude, BigDecimal longitude,
                                BigDecimal speedKmh, BigDecimal headingDeg) {
        Optional<Trip> tripOpt = tripRepository
                .findFirstByFleetAssetIdAndStatusAndEntityStatusNotOrderByStartedAtDesc(
                        fleetAssetId, TripStatus.IN_TRANSIT, EntityStatus.DELETED);
        if (tripOpt.isEmpty()) {
            log.debug("MQTT ingest: no IN_TRANSIT trip for assetId={}", fleetAssetId);
            return;
        }
        processLiveTelemetry(tripOpt.get(), latitude, longitude, speedKmh, headingDeg);
    }

    // ================================================================
    // SHARED PROCESSING LOGIC
    // ================================================================

    private void processLiveTelemetry(Trip trip,
                                      BigDecimal latitude, BigDecimal longitude,
                                      BigDecimal speedKmh, BigDecimal headingDeg) {

        // STEP 4 — Ensure route plan exists
        TripRoutePlan plan = routePlannerSupport.ensureRoutePlan(trip, INGEST_ACTOR);

        // STEP 5 — Disable simulation if active
        if (plan.isSimulationActive()) {
            plan.setSimulationActive(false);
        }

        // STEP 6 — Update plan position, speed, heading
        if (latitude != null) plan.setCurrentLatitude(latitude);
        if (longitude != null) plan.setCurrentLongitude(longitude);
        if (speedKmh != null) plan.setCurrentSpeedKmh(speedKmh);
        if (headingDeg != null) plan.setCurrentHeadingDeg(headingDeg);

        // STEP 7 — Compute overall progress using haversine along path
        if (latitude != null && longitude != null) {
            BigDecimal progress = computeProgress(plan, latitude, longitude);
            plan.setOverallProgressPct(progress);
        }
        plan.setModifiedAt(LocalDateTime.now());
        plan.setModifiedBy(INGEST_ACTOR);
        tripRoutePlanRepository.save(plan);

        // STEP 8 — Build snapshot and publish
        TripLiveSnapshotDto snapshot = buildSnapshot(trip, plan, speedKmh, headingDeg);
        telemetryPublisher.publish(trip, snapshot);

        // STEP 9 — Record CHECKPOINT trip event
        TripEvent event = new TripEvent();
        event.setTrip(trip);
        event.setEventType(TripEventType.CHECKPOINT);
        event.setEventTime(LocalDateTime.now());
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        event.setNotes("Telemetry checkpoint");
        event.setEntityStatus(EntityStatus.ACTIVE);
        event.setCreatedAt(LocalDateTime.now());
        event.setCreatedBy(INGEST_ACTOR);
        tripEventServiceAuditable.create(event, Locale.ENGLISH, INGEST_ACTOR);

        log.debug("Telemetry processed for trip id={} progress={}%",
                trip.getId(), plan.getOverallProgressPct());
    }

    private BigDecimal computeProgress(TripRoutePlan plan, BigDecimal lat, BigDecimal lng) {
        List<TripRouteWaypointDto> fullPath = routePlannerSupport.buildFullPath(plan);
        if (fullPath.size() < 2) {
            return plan.getOverallProgressPct();
        }
        BigDecimal totalDistanceKm = routePlannerSupport.estimateTotalDistanceKm(fullPath);
        if (totalDistanceKm == null || totalDistanceKm.compareTo(BigDecimal.ZERO) == 0) {
            return plan.getOverallProgressPct();
        }

        // Find how far along the route the current position is
        // by accumulating distances from origin until the closest segment
        TripRouteWaypointDto current = new TripRouteWaypointDto();
        current.setLatitude(lat);
        current.setLongitude(lng);

        double distanceCoveredKm = 0;
        double closestProjection = 0;
        double minDistToSegment = Double.MAX_VALUE;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            TripRouteWaypointDto from = fullPath.get(i);
            TripRouteWaypointDto to = fullPath.get(i + 1);
            double segLen = routePlannerSupport.haversineKm(from, to);
            double distToPoint = routePlannerSupport.haversineKm(from, current);
            if (distToPoint < minDistToSegment) {
                minDistToSegment = distToPoint;
                closestProjection = distanceCoveredKm + Math.min(distToPoint, segLen);
            }
            distanceCoveredKm += segLen;
        }

        double progressDouble = (closestProjection / totalDistanceKm.doubleValue()) * 100.0;
        progressDouble = Math.min(100.0, Math.max(0.0, progressDouble));
        return BigDecimal.valueOf(progressDouble).setScale(2, RoundingMode.HALF_UP);
    }

    private TripLiveSnapshotDto buildSnapshot(Trip trip, TripRoutePlan plan,
                                              BigDecimal speedKmh, BigDecimal headingDeg) {
        TripLiveSnapshotDto snapshot = new TripLiveSnapshotDto();
        snapshot.setTripId(trip.getId());
        snapshot.setTripNumber(trip.getTripNumber());
        snapshot.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);
        snapshot.setFromWarehouseName(trip.getFromWarehouseName());
        snapshot.setToWarehouseName(trip.getToWarehouseName());
        snapshot.setLatitude(plan.getCurrentLatitude());
        snapshot.setLongitude(plan.getCurrentLongitude());
        snapshot.setSpeedKmh(speedKmh != null ? speedKmh : plan.getCurrentSpeedKmh());
        snapshot.setHeadingDeg(headingDeg != null ? headingDeg : plan.getCurrentHeadingDeg());
        snapshot.setOverallProgressPct(plan.getOverallProgressPct());
        snapshot.setSimulationActive(false);
        snapshot.setMoving(speedKmh != null && speedKmh.compareTo(BigDecimal.ONE) > 0);
        return snapshot;
    }

    private void markDeviceTelemetry(Long deviceId) {
        if (deviceId == null) return;
        try {
            fleetManagementServiceClient.markDeviceTelemetry(deviceId);
        } catch (Exception ex) {
            log.warn("Failed to mark telemetry on device id={}: {}", deviceId, ex.getMessage());
        }
    }

    private IngestTelemetryResponse errorResponse(int statusCode, String message) {
        IngestTelemetryResponse response = new IngestTelemetryResponse();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }
}
