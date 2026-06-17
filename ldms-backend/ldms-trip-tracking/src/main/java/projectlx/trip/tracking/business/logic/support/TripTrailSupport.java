package projectlx.trip.tracking.business.logic.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.utils.dtos.TripRouteWaypointDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a capped GPS trail on {@link TripRoutePlan#trailJson} for live maps and trip history.
 */
@RequiredArgsConstructor
@Slf4j
public class TripTrailSupport {

    public static final int MAX_TRAIL_POINTS = 500;

    private final ObjectMapper objectMapper;

    public void appendTrailPoint(TripRoutePlan plan, BigDecimal latitude, BigDecimal longitude, BigDecimal speedKmh) {
        if (latitude == null || longitude == null) {
            return;
        }
        List<Map<String, Object>> trail = readRawTrail(plan);
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("latitude", latitude);
        point.put("longitude", longitude);
        point.put("speedKmh", speedKmh);
        point.put("recordedAt", LocalDateTime.now().toString());
        trail.add(point);
        while (trail.size() > MAX_TRAIL_POINTS) {
            trail.remove(0);
        }
        writeRawTrail(plan, trail);
    }

    public List<TripRouteWaypointDto> parseTrail(TripRoutePlan plan) {
        List<TripRouteWaypointDto> waypoints = new ArrayList<>();
        for (Map<String, Object> point : readRawTrail(plan)) {
            TripRouteWaypointDto dto = new TripRouteWaypointDto();
            dto.setType("TRAIL");
            dto.setLabel("Trail point");
            dto.setLatitude(toBigDecimal(point.get("latitude")));
            dto.setLongitude(toBigDecimal(point.get("longitude")));
            dto.setSpeedKmh(toBigDecimal(point.get("speedKmh")));
            dto.setRecordedAt(parseRecordedAt(point.get("recordedAt")));
            waypoints.add(dto);
        }
        return waypoints;
    }

    public void clearTrail(TripRoutePlan plan) {
        plan.setTrailJson("[]");
    }

    private List<Map<String, Object>> readRawTrail(TripRoutePlan plan) {
        if (plan.getTrailJson() == null || plan.getTrailJson().isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(plan.getTrailJson(), new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Unable to parse trail JSON for trip {}: {}", plan.getTripId(), ex.getMessage());
            return new ArrayList<>();
        }
    }

    private void writeRawTrail(TripRoutePlan plan, List<Map<String, Object>> trail) {
        try {
            plan.setTrailJson(objectMapper.writeValueAsString(trail));
        } catch (Exception ex) {
            log.warn("Unable to serialise trail JSON for trip {}: {}", plan.getTripId(), ex.getMessage());
        }
    }

    private static LocalDateTime parseRecordedAt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
