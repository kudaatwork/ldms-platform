package projectlx.fuel.expenses.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fuel.expenses.business.auditable.api.FuelSessionServiceAuditable;
import projectlx.fuel.expenses.business.logic.api.FuelSessionService;
import projectlx.fuel.expenses.business.logic.api.FuelTelemetryLogService;
import projectlx.fuel.expenses.business.validator.api.FuelSessionServiceValidator;
import projectlx.fuel.expenses.model.FuelSession;
import projectlx.fuel.expenses.repository.FuelSessionRepository;
import projectlx.fuel.expenses.utils.dtos.FuelSessionDto;
import projectlx.fuel.expenses.utils.enums.FuelSessionStatus;
import projectlx.fuel.expenses.utils.enums.I18Code;
import projectlx.fuel.expenses.utils.responses.FuelSessionResponse;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Core fuel session business logic.
 *
 * Responsibilities:
 *  - Open a FuelSession when a trip starts (400 L tank, 100 % full, 35 L/100 km).
 *  - Consume trip.location_updated events: haversine distance → fuel deduction →
 *    persist → publish fuel.level_updated to fuel.exchange.
 *  - Serve the live snapshot via getLiveByTripId.
 *
 * Note: No @Service annotation — wired as a @Bean in BusinessConfig.
 */
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FuelSessionServiceImpl implements FuelSessionService {

    private static final BigDecimal EARTH_RADIUS_KM   = new BigDecimal("6371.0");
    private static final BigDecimal DEFAULT_TANK_CAP  = new BigDecimal("400.00");
    private static final BigDecimal DEFAULT_RATE       = new BigDecimal("35.00");
    private static final BigDecimal HUNDRED            = new BigDecimal("100");

    private static final String FUEL_EXCHANGE          = "fuel.exchange";
    private static final String ROUTING_KEY_LEVEL_UPDATED = "fuel.level_updated";

    private final FuelSessionServiceValidator fuelSessionServiceValidator;
    private final FuelSessionServiceAuditable fuelSessionServiceAuditable;
    private final FuelSessionRepository       fuelSessionRepository;
    private final RabbitTemplate              rabbitTemplate;
    private final MessageService              messageService;
    private final FuelTelemetryLogService     fuelTelemetryLogService;

    // ============================================================
    // TRIP STARTED — open a new fuel session
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void onTripStarted(Map<String, Object> payload) {

        Long tripId = extractLong(payload, "tripId");
        if (tripId == null) {
            log.warn("trip.started event missing tripId — skipping fuel session creation");
            return;
        }

        // Idempotency guard — a session may already exist if the event is replayed.
        if (fuelSessionRepository.existsByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)) {
            log.info("FuelSession already exists for tripId={} — skipping duplicate creation", tripId);
            return;
        }

        // ============================================================
        // STEP 1: Build the new session from the event payload
        // ============================================================
        FuelSession session = new FuelSession();
        session.setTripId(tripId);
        session.setOrganizationId(extractLong(payload, "organizationId"));
        session.setFleetAssetId(extractLong(payload, "fleetAssetId"));
        session.setFleetDriverId(extractLong(payload, "fleetDriverId"));
        session.setShipmentId(extractLong(payload, "shipmentId"));
        session.setTankCapacityLiters(DEFAULT_TANK_CAP);
        session.setFuelRemainingLiters(DEFAULT_TANK_CAP);
        session.setFuelLevelPct(HUNDRED);
        session.setConsumptionRateLPer100km(DEFAULT_RATE);
        session.setDistanceTravelledKm(BigDecimal.ZERO);
        session.setStatus(FuelSessionStatus.ACTIVE);
        session.setMoving(false);
        session.setEntityStatus(EntityStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setCreatedBy("trip.started-consumer");

        // Seed GPS position if the event carries it (optional).
        BigDecimal lat = extractDecimal(payload, "latitude");
        BigDecimal lng = extractDecimal(payload, "longitude");
        if (lat != null && lng != null) {
            session.setLastLatitude(lat);
            session.setLastLongitude(lng);
        }

        // ============================================================
        // STEP 2: Persist via auditable
        // ============================================================
        FuelSession saved = fuelSessionServiceAuditable.create(session, Locale.ENGLISH, "trip.started-consumer");
        log.info("FuelSession created: id={} tripId={} fuelLevel={}%", saved.getId(), tripId, saved.getFuelLevelPct());
    }

    // ============================================================
    // LOCATION UPDATED — calculate distance, deduct fuel, publish
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void onLocationUpdated(Map<String, Object> payload) {

        Long tripId  = extractLong(payload, "tripId");
        BigDecimal newLat = extractDecimal(payload, "latitude");
        BigDecimal newLng = extractDecimal(payload, "longitude");

        if (tripId == null || newLat == null || newLng == null) {
            log.warn("trip.location_updated missing required fields (tripId={} lat={} lng={}) — skipping",
                    tripId, newLat, newLng);
            return;
        }

        // ============================================================
        // STEP 1: Load the active session with a pessimistic write lock
        // ============================================================
        Optional<FuelSession> sessionOpt = fuelSessionRepository
                .findByTripIdAndStatusAndEntityStatusNot(tripId, FuelSessionStatus.ACTIVE, EntityStatus.DELETED);

        if (sessionOpt.isEmpty()) {
            log.warn("No active FuelSession for tripId={} — location update ignored", tripId);
            return;
        }

        FuelSession session = sessionOpt.get();

        // ============================================================
        // STEP 2: Haversine distance from last known position
        // ============================================================
        BigDecimal distanceKm = BigDecimal.ZERO;
        if (session.getLastLatitude() != null && session.getLastLongitude() != null) {
            distanceKm = haversineKm(
                    session.getLastLatitude(), session.getLastLongitude(),
                    newLat, newLng);
            log.debug("Haversine distance for tripId={}: {} km", tripId, distanceKm);
        }

        // ============================================================
        // STEP 3: Fuel deduction — rate is in L per 100 km
        // ============================================================
        if (distanceKm.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal consumed = session.getConsumptionRateLPer100km()
                    .multiply(distanceKm)
                    .divide(HUNDRED, 4, RoundingMode.HALF_UP);

            BigDecimal newRemaining = session.getFuelRemainingLiters()
                    .subtract(consumed)
                    .max(BigDecimal.ZERO);

            BigDecimal newPct = newRemaining
                    .divide(session.getTankCapacityLiters(), 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);

            session.setFuelRemainingLiters(newRemaining.setScale(2, RoundingMode.HALF_UP));
            session.setFuelLevelPct(newPct);
            session.setDistanceTravelledKm(
                    session.getDistanceTravelledKm().add(distanceKm).setScale(2, RoundingMode.HALF_UP));
        }

        // ============================================================
        // STEP 4: Update GPS + motion flag
        // ============================================================
        session.setLastLatitude(newLat);
        session.setLastLongitude(newLng);
        session.setMoving(true);
        session.setModifiedAt(LocalDateTime.now());
        session.setModifiedBy("trip.location_updated-consumer");

        FuelSession updated = fuelSessionServiceAuditable.update(session, Locale.ENGLISH,
                "trip.location_updated-consumer");

        // ============================================================
        // STEP 5: Publish fuel.level_updated to fuel.exchange
        // ============================================================
        try {
            Map<String, Object> event = buildFuelLevelUpdatedEvent(updated);
            rabbitTemplate.convertAndSend(FUEL_EXCHANGE, ROUTING_KEY_LEVEL_UPDATED, event);
            log.info("Published fuel.level_updated for tripId={} fuelLevel={}%",
                    tripId, updated.getFuelLevelPct());
        } catch (Exception ex) {
            log.error("Failed to publish fuel.level_updated for tripId={}: {}", tripId, ex.getMessage(), ex);
        }

        // ============================================================
        // STEP 6: Log CONSUMPTION_DELTA telemetry for audit / efficiency analysis
        // ============================================================
        if (distanceKm.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal consumed = session.getConsumptionRateLPer100km()
                    .multiply(distanceKm)
                    .divide(HUNDRED, 4, RoundingMode.HALF_UP);
            fuelTelemetryLogService.logConsumptionDelta(
                    tripId,
                    updated.getOrganizationId(),
                    updated.getFleetAssetId(),
                    updated.getId(),
                    distanceKm,
                    consumed,
                    updated.getFuelLevelPct(),
                    updated.getFuelRemainingLiters(),
                    newLat,
                    newLng);
        }
    }

    // ============================================================
    // LIVE SNAPSHOT — REST query
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public FuelSessionResponse getLiveByTripId(Long tripId, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = fuelSessionServiceValidator.isGetLiveByTripIdRequestValid(tripId, locale);
        if (!validation.getSuccess()) {
            FuelSessionResponse response = new FuelSessionResponse();
            response.setSuccess(false);
            response.setStatusCode(400);
            response.setErrorMessages(validation.getErrorMessages());
            return response;
        }

        // ============================================================
        // STEP 2: Load the session
        // ============================================================
        Optional<FuelSession> sessionOpt = fuelSessionRepository
                .findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED);

        if (sessionOpt.isEmpty()) {
            FuelSessionResponse response = new FuelSessionResponse();
            response.setSuccess(false);
            response.setStatusCode(404);
            response.setErrorMessages(List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUEL_SESSION_NOT_FOUND.getCode(),
                            new String[]{String.valueOf(tripId)}, locale)));
            return response;
        }

        // ============================================================
        // STEP 3: Map to DTO and return
        // ============================================================
        FuelSessionDto dto = mapToDto(sessionOpt.get());

        FuelSessionResponse response = new FuelSessionResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setFuelSessionDto(dto);
        return response;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Haversine formula — returns great-circle distance in kilometres.
     */
    private BigDecimal haversineKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double φ1 = Math.toRadians(lat1.doubleValue());
        double φ2 = Math.toRadians(lat2.doubleValue());
        double Δφ = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double Δλ = Math.toRadians(lon2.subtract(lon1).doubleValue());

        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2)
                + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distKm = EARTH_RADIUS_KM.doubleValue() * c;
        return new BigDecimal(distKm, MathContext.DECIMAL64).setScale(4, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildFuelLevelUpdatedEvent(FuelSession session) {
        Map<String, Object> event = new HashMap<>();
        event.put("tripId",              session.getTripId());
        event.put("organizationId",      session.getOrganizationId());
        event.put("fleetAssetId",        session.getFleetAssetId());
        event.put("fuelRemainingLiters", session.getFuelRemainingLiters());
        event.put("fuelLevelPct",        session.getFuelLevelPct());
        event.put("distanceTravelledKm", session.getDistanceTravelledKm());
        event.put("lastLatitude",        session.getLastLatitude());
        event.put("lastLongitude",       session.getLastLongitude());
        event.put("moving",              session.isMoving());
        return event;
    }

    private FuelSessionDto mapToDto(FuelSession session) {
        FuelSessionDto dto = new FuelSessionDto();
        dto.setId(session.getId());
        dto.setTripId(session.getTripId());
        dto.setOrganizationId(session.getOrganizationId());
        dto.setFleetAssetId(session.getFleetAssetId());
        dto.setFleetDriverId(session.getFleetDriverId());
        dto.setShipmentId(session.getShipmentId());
        dto.setTankCapacityLiters(session.getTankCapacityLiters());
        dto.setFuelRemainingLiters(session.getFuelRemainingLiters());
        dto.setFuelLevelPct(session.getFuelLevelPct());
        dto.setConsumptionRateLPer100km(session.getConsumptionRateLPer100km());
        dto.setDistanceTravelledKm(session.getDistanceTravelledKm());
        dto.setLastLatitude(session.getLastLatitude());
        dto.setLastLongitude(session.getLastLongitude());
        dto.setStatus(session.getStatus());
        dto.setMoving(session.isMoving());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setCreatedBy(session.getCreatedBy());
        dto.setModifiedAt(session.getModifiedAt());
        dto.setModifiedBy(session.getModifiedBy());
        return dto;
    }

    private static Long extractLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal extractDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
