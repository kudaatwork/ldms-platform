package projectlx.trip.tracking.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.trip.tracking.clients.FleetManagementServiceClient;
import projectlx.trip.tracking.clients.ShipmentManagementServiceClient;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.utils.dtos.FleetAssetSummaryDto;
import projectlx.trip.tracking.utils.dtos.FleetDriverSummaryDto;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.responses.FleetAssetFeignResponse;
import projectlx.trip.tracking.utils.responses.FleetDriverFeignResponse;
import projectlx.trip.tracking.utils.responses.ShipmentFeignResponse;
import projectlx.trip.tracking.utils.dtos.ShipmentSummaryDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class TripLiveSnapshotEnricher {

    private final FleetManagementServiceClient fleetManagementServiceClient;
    private final ShipmentManagementServiceClient shipmentManagementServiceClient;
    private final TripTrailSupport tripTrailSupport;
    private final TripJourneyTimingSupport journeyTimingSupport;
    private final TripRoutePlanRepository tripRoutePlanRepository;

    public void enrich(Trip trip, TripRoutePlan plan, TripLiveSnapshotDto snapshot) {
        if (trip == null || snapshot == null) {
            return;
        }
        snapshot.setFleetAssetId(trip.getFleetAssetId());
        boolean moving = snapshot.isMoving();
        if (plan != null) {
            snapshot.setSimulationPaused(plan.isSimulationPaused());
            snapshot.setDistanceTravelledKm(plan.getDistanceTravelledKm());
            snapshot.setTrail(tripTrailSupport.parseTrail(plan));
            snapshot.setOnBreak(plan.isSimulationPaused() || trip.getStatus() == TripStatus.ROADSIDE_HOLD);
            snapshot.setCurrentSegmentIndex(plan.getCurrentSegmentIndex());
            snapshot.setSegmentProgressPct(plan.getSegmentProgressPct());
            applyWaypointProgress(snapshot, plan.getCurrentSegmentIndex(), plan.getOverallProgressPct());
            moving = plan.isSimulationActive() && !plan.isSimulationPaused()
                    && trip.getStatus() == TripStatus.IN_TRANSIT
                    && snapshot.getSpeedKmh() != null
                    && snapshot.getSpeedKmh().compareTo(BigDecimal.ZERO) > 0;
            snapshot.setMoving(moving);
            journeyTimingSupport.flush(plan, moving, java.time.LocalDateTime.now());
            tripRoutePlanRepository.save(plan);
        }
        journeyTimingSupport.populateSnapshot(trip, plan, snapshot, moving);
        applyCargoFields(trip, snapshot);
        applyFleetAsset(trip, snapshot);
        applyFleetDriver(trip, snapshot);
        if (plan != null) {
            BigDecimal distanceTravelledKm = plan.getDistanceTravelledKm();
            if (distanceTravelledKm != null && distanceTravelledKm.compareTo(BigDecimal.ZERO) > 0) {
                TripSimulationFuelSupport.applyToSnapshot(snapshot, distanceTravelledKm);
            }
        }
        if (snapshot.getMaxSpeedKmh() != null && snapshot.getSpeedKmh() != null) {
            snapshot.setSpeedLimitExceeded(snapshot.getSpeedKmh().compareTo(snapshot.getMaxSpeedKmh()) > 0);
        }
        applyProximity(trip, plan, snapshot);
    }

    /**
     * Computes the straight-line distance from the current position to the route's destination.
     * Sets nearDestination=true when within 2 km, and arrivalPromptVisible=true when IN_TRANSIT and nearDestination.
     *
     * Uses the Haversine formula (Earth radius 6371 km).
     */
    private void applyProximity(Trip trip, TripRoutePlan plan, TripLiveSnapshotDto snapshot) {
        if (plan == null
                || snapshot.getLatitude() == null || snapshot.getLongitude() == null
                || plan.getDestinationLatitude() == null || plan.getDestinationLongitude() == null) {
            return;
        }
        BigDecimal distKm = haversineKm(
                snapshot.getLatitude(), snapshot.getLongitude(),
                plan.getDestinationLatitude(), plan.getDestinationLongitude());
        snapshot.setDestinationDistanceKm(distKm);
        boolean near = distKm.compareTo(NEAR_DESTINATION_KM) <= 0;
        snapshot.setNearDestination(near);
        snapshot.setArrivalPromptVisible(near && trip.getStatus() == TripStatus.IN_TRANSIT);
    }

    private static final BigDecimal EARTH_RADIUS_KM = new BigDecimal("6371");
    private static final BigDecimal NEAR_DESTINATION_KM = new BigDecimal("2");
    private static final double DEG_TO_RAD = Math.PI / 180.0;

    private static BigDecimal haversineKm(BigDecimal lat1, BigDecimal lon1,
                                          BigDecimal lat2, BigDecimal lon2) {
        double dLat = (lat2.doubleValue() - lat1.doubleValue()) * DEG_TO_RAD;
        double dLon = (lon2.doubleValue() - lon1.doubleValue()) * DEG_TO_RAD;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1.doubleValue() * DEG_TO_RAD) * Math.cos(lat2.doubleValue() * DEG_TO_RAD)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(EARTH_RADIUS_KM.doubleValue() * c)
                .round(new MathContext(6));
    }

    private void applyCargoFields(Trip trip, TripLiveSnapshotDto snapshot) {
        snapshot.setShipmentId(trip.getShipmentId());
        snapshot.setShipmentNumber(trip.getShipmentNumber());
        snapshot.setProductName(trip.getProductName());
        snapshot.setProductCode(trip.getProductCode());
        snapshot.setQuantity(trip.getQuantity());
        if (snapshot.getQuantity() != null || trip.getShipmentId() == null) {
            return;
        }
        try {
            ShipmentFeignResponse response = shipmentManagementServiceClient.findShipmentById(
                    trip.getShipmentId(), Locale.ENGLISH);
            ShipmentSummaryDto shipment = response != null ? response.getShipmentDto() : null;
            if (shipment == null) {
                return;
            }
            if (snapshot.getShipmentNumber() == null) {
                snapshot.setShipmentNumber(shipment.getShipmentNumber());
            }
            if (snapshot.getProductName() == null) {
                snapshot.setProductName(shipment.getProductName());
            }
            snapshot.setProductCode(shipment.getProductCode());
            snapshot.setQuantity(shipment.getQuantity());
        } catch (Exception ex) {
            log.debug("Shipment cargo enrich skipped for trip {}: {}", trip.getId(), ex.getMessage());
        }
    }

    private void applyFleetAsset(Trip trip, TripLiveSnapshotDto snapshot) {
        if (trip.getFleetAssetId() == null) {
            return;
        }
        try {
            FleetAssetFeignResponse response = fleetManagementServiceClient.findFleetAssetById(
                    trip.getFleetAssetId(), Locale.ENGLISH);
            FleetAssetSummaryDto asset = response != null ? response.getFleetAssetDto() : null;
            if (asset == null) {
                return;
            }
            snapshot.setVehicleRegistration(asset.getRegistration());
            snapshot.setMaxSpeedKmh(asset.getMaxSpeedKmh());
            if (snapshot.getDriverName() == null) {
                snapshot.setDriverName(asset.getDriverName());
            }
        } catch (Exception ex) {
            log.debug("Fleet asset enrich skipped for trip {}: {}", trip.getId(), ex.getMessage());
        }
    }

    private void applyFleetDriver(Trip trip, TripLiveSnapshotDto snapshot) {
        if (trip.getFleetDriverId() == null) {
            return;
        }
        try {
            FleetDriverFeignResponse response = fleetManagementServiceClient.findFleetDriverById(
                    trip.getFleetDriverId(), Locale.ENGLISH);
            FleetDriverSummaryDto driver = response != null ? response.getFleetDriverDto() : null;
            if (driver == null) {
                return;
            }
            String name = buildDriverName(driver.getFirstName(), driver.getLastName());
            if (name != null) {
                snapshot.setDriverName(name);
            }
        } catch (Exception ex) {
            log.debug("Fleet driver enrich skipped for trip {}: {}", trip.getId(), ex.getMessage());
        }
    }

    private static String buildDriverName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        String combined = (f + " " + l).trim();
        return combined.isEmpty() ? null : combined;
    }

    private void applyWaypointProgress(TripLiveSnapshotDto snapshot, int segmentIndex, BigDecimal overallProgressPct) {
        int total = snapshot.getRouteWaypoints() != null ? snapshot.getRouteWaypoints().size() : 0;
        snapshot.setTotalWaypointCount(total);
        if (total <= 0) {
            snapshot.setCompletedWaypointCount(0);
            return;
        }
        if (overallProgressPct != null && overallProgressPct.compareTo(new BigDecimal("100")) >= 0) {
            snapshot.setCompletedWaypointCount(total);
            return;
        }
        int completed = Math.min(Math.max(segmentIndex, 0) + 1, total);
        snapshot.setCompletedWaypointCount(completed);
    }
}
