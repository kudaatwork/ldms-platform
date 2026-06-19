package projectlx.trip.tracking.business.logic.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.clients.InventoryManagementServiceClient;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.utils.dtos.InventoryRouteStopFeignDto;
import projectlx.trip.tracking.utils.dtos.InventoryRouteStopListFeignResponse;
import projectlx.trip.tracking.utils.dtos.TripRouteWaypointDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Builds corridor route plans using real Zimbabwe GPS coordinates on the Harare–Bulawayo freight corridor.
 * When the trip has an inventoryTransferId or salesOrderId, real route stops are fetched from inventory-management.
 * Falls back to hardcoded corridor when no stops are available.
 */
@Slf4j
public class TripRoutePlannerSupport {

    private static final BigDecimal HARARE_MSASA_LAT = new BigDecimal("-17.7847000");
    private static final BigDecimal HARARE_MSASA_LNG = new BigDecimal("31.0504000");
    private static final BigDecimal BULAWAYO_WH_LAT = new BigDecimal("-20.1532000");
    private static final BigDecimal BULAWAYO_WH_LNG = new BigDecimal("28.5802000");

    private final TripRoutePlanRepository tripRoutePlanRepository;
    private final ObjectMapper objectMapper;
    private final Optional<InventoryManagementServiceClient> inventoryClient;

    public TripRoutePlannerSupport(TripRoutePlanRepository tripRoutePlanRepository,
                                   ObjectMapper objectMapper) {
        this.tripRoutePlanRepository = tripRoutePlanRepository;
        this.objectMapper = objectMapper;
        this.inventoryClient = Optional.empty();
    }

    public TripRoutePlannerSupport(TripRoutePlanRepository tripRoutePlanRepository,
                                   ObjectMapper objectMapper,
                                   InventoryManagementServiceClient inventoryClient) {
        this.tripRoutePlanRepository = tripRoutePlanRepository;
        this.objectMapper = objectMapper;
        this.inventoryClient = Optional.ofNullable(inventoryClient);
    }

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

        // ============================================================
        // STEP 1: Attempt to load real route stops from inventory-management
        // ============================================================
        List<TripRouteWaypointDto> corridor = buildCorridorFromInventoryStops(trip, originLabel, destLabel);

        // ============================================================
        // STEP 2: Build and persist route plan
        // ============================================================
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

    /**
     * Build corridor waypoints from real inventory route stops when available.
     * Interpolates lat/lng between origin and destination based on stop sequence fraction.
     * Falls back to the hardcoded Harare–Bulawayo corridor when no stops are found.
     */
    private List<TripRouteWaypointDto> buildCorridorFromInventoryStops(
            Trip trip, String originLabel, String destLabel) {

        if (inventoryClient.isEmpty()) {
            return hardcodedCorridor();
        }

        String contextType = null;
        Long contextId = null;

        if (trip.getInventoryTransferId() != null) {
            contextType = "INVENTORY_TRANSFER";
            contextId = trip.getInventoryTransferId();
        } else if (trip.getSalesOrderId() != null) {
            contextType = "SALES_ORDER";
            contextId = trip.getSalesOrderId();
        }

        if (contextType == null) {
            return hardcodedCorridor();
        }

        try {
            InventoryRouteStopListFeignResponse response =
                    inventoryClient.get().findRouteStopsByContext(contextType, contextId, Locale.ENGLISH);

            if (response == null || !response.isSuccess()
                    || response.getLogisticsRouteStopDtoList() == null
                    || response.getLogisticsRouteStopDtoList().isEmpty()) {
                log.debug("No route stops found for contextType={} contextId={}; using hardcoded corridor.",
                        contextType, contextId);
                return hardcodedCorridor();
            }

            List<InventoryRouteStopFeignDto> stops = response.getLogisticsRouteStopDtoList().stream()
                    .filter(stop -> "EN_ROUTE_DEPOT".equalsIgnoreCase(stop.getStopType()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

            if (stops.isEmpty()) {
                log.debug("No en-route depot stops for contextType={} contextId={}; using hardcoded corridor.",
                        contextType, contextId);
                return hardcodedCorridor();
            }

            Collections.sort(stops, (a, b) -> {
                int seqA = a.getStopSequence() != null ? a.getStopSequence() : 0;
                int seqB = b.getStopSequence() != null ? b.getStopSequence() : 0;
                return Integer.compare(seqA, seqB);
            });

            int total = stops.size() + 1;
            List<TripRouteWaypointDto> waypoints = new ArrayList<>();

            for (int i = 0; i < stops.size(); i++) {
                InventoryRouteStopFeignDto stop = stops.get(i);
                String label = stop.getLocationLabel() != null ? stop.getLocationLabel()
                        : "Depot " + (i + 1);
                // Fraction along the origin→destination segment
                double fraction = (double) (i + 1) / total;
                BigDecimal lat = interpolate(HARARE_MSASA_LAT, BULAWAYO_WH_LAT, fraction);
                BigDecimal lng = interpolate(HARARE_MSASA_LNG, BULAWAYO_WH_LNG, fraction);
                waypoints.add(waypoint(label, lat, lng, "CHECKPOINT"));
            }

            log.info("Built {} real route stops for contextType={} contextId={}",
                    waypoints.size(), contextType, contextId);
            return waypoints;

        } catch (Exception ex) {
            log.warn("Failed to fetch route stops from inventory [contextType={} contextId={}]: {}; falling back to hardcoded corridor.",
                    contextType, contextId, ex.getMessage());
            return hardcodedCorridor();
        }
    }

    private List<TripRouteWaypointDto> hardcodedCorridor() {
        return List.of(
                waypoint("Chitungwiza Bypass", new BigDecimal("-18.0128000"), new BigDecimal("31.0756000"), "CHECKPOINT"),
                waypoint("Norton Toll Plaza", new BigDecimal("-17.8833000"), new BigDecimal("30.7000000"), "CHECKPOINT"),
                waypoint("Kadoma Logistics Hub", new BigDecimal("-18.3333000"), new BigDecimal("29.9153000"), "CHECKPOINT"),
                waypoint("Gweru Corridor Stop", new BigDecimal("-19.4500000"), new BigDecimal("29.8167000"), "CHECKPOINT"),
                waypoint("Shangani River Crossing", new BigDecimal("-19.7833000"), new BigDecimal("29.3500000"), "CHECKPOINT")
        );
    }

    private static BigDecimal interpolate(BigDecimal start, BigDecimal end, double fraction) {
        double result = start.doubleValue() + (end.doubleValue() - start.doubleValue()) * fraction;
        return BigDecimal.valueOf(result).setScale(7, RoundingMode.HALF_UP);
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
