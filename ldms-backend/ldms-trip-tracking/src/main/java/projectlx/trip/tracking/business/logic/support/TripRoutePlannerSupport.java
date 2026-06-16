package projectlx.trip.tracking.business.logic.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.utils.dtos.TripRouteWaypointDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds corridor route plans using real Zimbabwe GPS coordinates on the Harare–Bulawayo freight corridor.
 * Origin/destination labels come from the trip's warehouse names when available.
 */
@RequiredArgsConstructor
@Slf4j
public class TripRoutePlannerSupport {

    private static final BigDecimal HARARE_MSASA_LAT = new BigDecimal("-17.7847000");
    private static final BigDecimal HARARE_MSASA_LNG = new BigDecimal("31.0504000");
    private static final BigDecimal BULAWAYO_WH_LAT = new BigDecimal("-20.1532000");
    private static final BigDecimal BULAWAYO_WH_LNG = new BigDecimal("28.5802000");

    private final TripRoutePlanRepository tripRoutePlanRepository;
    private final ObjectMapper objectMapper;

    public TripRoutePlan ensureRoutePlan(Trip trip, String username) {
        return tripRoutePlanRepository.findByTripIdAndEntityStatusNot(trip.getId(), EntityStatus.DELETED)
                .orElseGet(() -> createRoutePlan(trip, username));
    }

    public List<TripRouteWaypointDto> parseWaypoints(TripRoutePlan plan) {
        try {
            return objectMapper.readValue(plan.getWaypointsJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse waypoints for trip {}: {}", plan.getTripId(), ex.getMessage());
            return List.of();
        }
    }

    public List<TripRouteWaypointDto> buildFullPath(TripRoutePlan plan) {
        List<TripRouteWaypointDto> path = new ArrayList<>();
        path.add(waypoint(plan.getOriginLabel(), plan.getOriginLatitude(), plan.getOriginLongitude(), "ORIGIN"));
        path.addAll(parseWaypoints(plan));
        path.add(waypoint(plan.getDestinationLabel(), plan.getDestinationLatitude(), plan.getDestinationLongitude(), "DESTINATION"));
        return path;
    }

    public BigDecimal estimateTotalDistanceKm(List<TripRouteWaypointDto> path) {
        if (path.size() < 2) {
            return BigDecimal.ZERO;
        }
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            total += haversineKm(path.get(i), path.get(i + 1));
        }
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    public double haversineKm(TripRouteWaypointDto from, TripRouteWaypointDto to) {
        double lat1 = from.getLatitude().doubleValue();
        double lon1 = from.getLongitude().doubleValue();
        double lat2 = to.getLatitude().doubleValue();
        double lon2 = to.getLongitude().doubleValue();
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public BigDecimal bearingDegrees(TripRouteWaypointDto from, TripRouteWaypointDto to) {
        double lat1 = Math.toRadians(from.getLatitude().doubleValue());
        double lat2 = Math.toRadians(to.getLatitude().doubleValue());
        double dLon = Math.toRadians(to.getLongitude().doubleValue() - from.getLongitude().doubleValue());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return BigDecimal.valueOf((brng + 360) % 360).setScale(2, RoundingMode.HALF_UP);
    }

    private TripRoutePlan createRoutePlan(Trip trip, String username) {
        LocalDateTime now = LocalDateTime.now();
        String originLabel = defaultLabel(trip.getFromWarehouseName(), "Harare Distribution Centre");
        String destLabel = defaultLabel(trip.getToWarehouseName(), "Bulawayo Regional Depot");

        List<TripRouteWaypointDto> corridor = List.of(
                waypoint("Chitungwiza Bypass", new BigDecimal("-18.0128000"), new BigDecimal("31.0756000"), "CHECKPOINT"),
                waypoint("Norton Toll Plaza", new BigDecimal("-17.8833000"), new BigDecimal("30.7000000"), "CHECKPOINT"),
                waypoint("Kadoma Logistics Hub", new BigDecimal("-18.3333000"), new BigDecimal("29.9153000"), "CHECKPOINT"),
                waypoint("Gweru Corridor Stop", new BigDecimal("-19.4500000"), new BigDecimal("29.8167000"), "CHECKPOINT"),
                waypoint("Shangani River Crossing", new BigDecimal("-19.7833000"), new BigDecimal("29.3500000"), "CHECKPOINT")
        );

        TripRoutePlan plan = new TripRoutePlan();
        plan.setTripId(trip.getId());
        plan.setOrganizationId(trip.getOrganizationId());
        plan.setOriginLabel(originLabel);
        plan.setDestinationLabel(destLabel);
        plan.setOriginLatitude(HARARE_MSASA_LAT);
        plan.setOriginLongitude(HARARE_MSASA_LNG);
        plan.setDestinationLatitude(BULAWAYO_WH_LAT);
        plan.setDestinationLongitude(BULAWAYO_WH_LNG);
        plan.setWaypointsJson(writeJson(corridor));
        plan.setSimulationActive(false);
        plan.setCurrentSegmentIndex(0);
        plan.setSegmentProgressPct(BigDecimal.ZERO);
        plan.setOverallProgressPct(BigDecimal.ZERO);
        plan.setCurrentLatitude(HARARE_MSASA_LAT);
        plan.setCurrentLongitude(HARARE_MSASA_LNG);
        plan.setEntityStatus(EntityStatus.ACTIVE);
        plan.setCreatedAt(now);
        plan.setCreatedBy(username);

        List<TripRouteWaypointDto> fullPath = new ArrayList<>();
        fullPath.add(waypoint(originLabel, HARARE_MSASA_LAT, HARARE_MSASA_LNG, "ORIGIN"));
        fullPath.addAll(corridor);
        fullPath.add(waypoint(destLabel, BULAWAYO_WH_LAT, BULAWAYO_WH_LNG, "DESTINATION"));
        plan.setTotalDistanceKm(estimateTotalDistanceKm(fullPath));

        return tripRoutePlanRepository.save(plan);
    }

    private String writeJson(List<TripRouteWaypointDto> waypoints) {
        try {
            return objectMapper.writeValueAsString(waypoints);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialise corridor waypoints", ex);
        }
    }

    private static String defaultLabel(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private static TripRouteWaypointDto waypoint(String label, BigDecimal lat, BigDecimal lng, String type) {
        TripRouteWaypointDto dto = new TripRouteWaypointDto();
        dto.setLabel(label);
        dto.setLatitude(lat);
        dto.setLongitude(lng);
        dto.setType(type);
        return dto;
    }
}
