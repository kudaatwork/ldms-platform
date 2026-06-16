package projectlx.fuel.expenses.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fuel.expenses.business.auditable.api.FuelTelemetryLogServiceAuditable;
import projectlx.fuel.expenses.business.logic.api.FuelTelemetryLogService;
import projectlx.fuel.expenses.business.logic.support.CallerOrganizationResolver;
import projectlx.fuel.expenses.business.logic.support.FuelExpensesMapper;
import projectlx.fuel.expenses.business.validator.api.FuelTelemetryLogServiceValidator;
import projectlx.fuel.expenses.model.FuelTelemetryLog;
import projectlx.fuel.expenses.repository.FuelTelemetryLogRepository;
import projectlx.fuel.expenses.utils.dtos.FuelTelemetryLogDto;
import projectlx.fuel.expenses.utils.enums.FuelReadingType;
import projectlx.fuel.expenses.utils.enums.FuelTelemetrySource;
import projectlx.fuel.expenses.utils.enums.I18Code;
import projectlx.fuel.expenses.utils.requests.RecordFuelTelemetryRequest;
import projectlx.fuel.expenses.utils.responses.FuelTelemetryLogResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Business logic for fuel telemetry logging.
 *
 * Responsibilities:
 *  - Accept driver-app / telematics / manual readings via REST.
 *  - Accept internal SYSTEM readings (consumption delta, top-up) from FuelSessionServiceImpl.
 *  - Provide paginated query by trip ID.
 *
 * Note: No @Service — wired in BusinessConfig.
 */
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FuelTelemetryLogServiceImpl implements FuelTelemetryLogService {

    private final FuelTelemetryLogServiceValidator fuelTelemetryLogServiceValidator;
    private final FuelTelemetryLogServiceAuditable  fuelTelemetryLogServiceAuditable;
    private final FuelTelemetryLogRepository        fuelTelemetryLogRepository;
    private final CallerOrganizationResolver        callerOrganizationResolver;
    private final FuelExpensesMapper                fuelExpensesMapper;
    private final MessageService                    messageService;

    // ============================================================
    // RECORD — driver app / telematics / manual submission
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FuelTelemetryLogResponse record(RecordFuelTelemetryRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = fuelTelemetryLogServiceValidator.isRecordTelemetryRequestValid(request, locale);
        if (!validation.getSuccess()) {
            FuelTelemetryLogResponse response = new FuelTelemetryLogResponse();
            response.setSuccess(false);
            response.setStatusCode(400);
            response.setErrorMessages(validation.getErrorMessages());
            return response;
        }

        // ============================================================
        // STEP 2: Resolve caller organisation
        // ============================================================
        Long organizationId = callerOrganizationResolver.resolveCallerOrganizationId(username);

        // ============================================================
        // STEP 3: Build and persist the telemetry log entry
        // ============================================================
        FuelTelemetryLog entry = buildEntry(
                request.getTripId(),
                organizationId,
                request.getFleetAssetId(),
                null,
                request.getSource(),
                request.getReadingType(),
                request.getFuelLevelPct(),
                request.getFuelLiters(),
                request.getOdometerKm(),
                request.getLatitude(),
                request.getLongitude(),
                null,
                null,
                request.getRecordedAt() != null ? request.getRecordedAt() : LocalDateTime.now(),
                request.getNotes(),
                username);

        FuelTelemetryLog saved = fuelTelemetryLogServiceAuditable.create(entry, locale, username);
        log.info("FuelTelemetryLog recorded: id={} tripId={} source={} type={}",
                saved.getId(), saved.getTripId(), saved.getSource(), saved.getReadingType());

        FuelTelemetryLogResponse response = new FuelTelemetryLogResponse();
        response.setSuccess(true);
        response.setStatusCode(201);
        response.setFuelTelemetryLogDto(fuelExpensesMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // FIND BY TRIP — paginated
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public FuelTelemetryLogResponse findByTripId(Long tripId, int page, int size, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = fuelTelemetryLogServiceValidator.isFindByTripIdRequestValid(tripId, locale);
        if (!validation.getSuccess()) {
            FuelTelemetryLogResponse response = new FuelTelemetryLogResponse();
            response.setSuccess(false);
            response.setStatusCode(400);
            response.setErrorMessages(validation.getErrorMessages());
            return response;
        }

        // ============================================================
        // STEP 2: Query
        // ============================================================
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
        Page<FuelTelemetryLog> resultPage = fuelTelemetryLogRepository
                .findByTripIdAndEntityStatusNotOrderByRecordedAtDesc(tripId, EntityStatus.DELETED, pageable);

        if (resultPage.isEmpty()) {
            FuelTelemetryLogResponse response = new FuelTelemetryLogResponse();
            response.setSuccess(false);
            response.setStatusCode(404);
            response.setErrorMessages(List.of(
                    messageService.getMessage(I18Code.MESSAGE_TELEMETRY_LOG_NOT_FOUND.getCode(),
                            new String[]{String.valueOf(tripId)}, locale)));
            return response;
        }

        // ============================================================
        // STEP 3: Map and return
        // ============================================================
        List<FuelTelemetryLogDto> dtoList = resultPage.getContent().stream()
                .map(fuelExpensesMapper::toDto)
                .collect(Collectors.toList());

        FuelTelemetryLogResponse response = new FuelTelemetryLogResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setFuelTelemetryLogDtoList(dtoList);
        response.setTotalElements(resultPage.getTotalElements());
        response.setTotalPages(resultPage.getTotalPages());
        return response;
    }

    // ============================================================
    // INTERNAL — consumption delta (called by FuelSessionServiceImpl)
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void logConsumptionDelta(Long tripId, Long organizationId, Long fleetAssetId, Long fuelSessionId,
                                    BigDecimal distanceDeltaKm, BigDecimal consumedLiters,
                                    BigDecimal fuelLevelPct, BigDecimal fuelRemainingLiters,
                                    BigDecimal latitude, BigDecimal longitude) {
        try {
            FuelTelemetryLog entry = buildEntry(
                    tripId, organizationId, fleetAssetId, fuelSessionId,
                    FuelTelemetrySource.SYSTEM,
                    FuelReadingType.CONSUMPTION_DELTA,
                    fuelLevelPct,
                    fuelRemainingLiters,
                    null,
                    latitude,
                    longitude,
                    distanceDeltaKm,
                    consumedLiters,
                    LocalDateTime.now(),
                    null,
                    "trip.location_updated-consumer");
            fuelTelemetryLogRepository.save(entry);
            log.debug("CONSUMPTION_DELTA logged for tripId={} distanceDelta={} consumed={}",
                    tripId, distanceDeltaKm, consumedLiters);
        } catch (Exception ex) {
            log.error("Failed to log CONSUMPTION_DELTA for tripId={}: {}", tripId, ex.getMessage(), ex);
        }
    }

    // ============================================================
    // INTERNAL — top-up applied (called by OperationalFundRequestServiceImpl)
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void logTopUp(Long tripId, Long organizationId, Long fleetAssetId, Long fuelSessionId,
                         BigDecimal approvedLiters, BigDecimal fuelLevelPct, BigDecimal fuelRemainingLiters) {
        try {
            FuelTelemetryLog entry = buildEntry(
                    tripId, organizationId, fleetAssetId, fuelSessionId,
                    FuelTelemetrySource.SYSTEM,
                    FuelReadingType.TOP_UP,
                    fuelLevelPct,
                    fuelRemainingLiters,
                    null,
                    null,
                    null,
                    null,
                    approvedLiters,
                    LocalDateTime.now(),
                    "Approved fund request top-up",
                    "fund-request-approver");
            fuelTelemetryLogRepository.save(entry);
            log.info("TOP_UP logged for tripId={} approvedLiters={}", tripId, approvedLiters);
        } catch (Exception ex) {
            log.error("Failed to log TOP_UP for tripId={}: {}", tripId, ex.getMessage(), ex);
        }
    }

    // ============================================================
    // HELPER
    // ============================================================

    private FuelTelemetryLog buildEntry(Long tripId, Long organizationId, Long fleetAssetId, Long fuelSessionId,
                                        FuelTelemetrySource source, FuelReadingType readingType,
                                        BigDecimal fuelLevelPct, BigDecimal fuelLiters, BigDecimal odometerKm,
                                        BigDecimal latitude, BigDecimal longitude,
                                        BigDecimal distanceDeltaKm, BigDecimal consumedLiters,
                                        LocalDateTime recordedAt, String notes, String createdBy) {
        FuelTelemetryLog entry = new FuelTelemetryLog();
        entry.setTripId(tripId);
        entry.setOrganizationId(organizationId != null ? organizationId : 0L);
        entry.setFleetAssetId(fleetAssetId);
        entry.setFuelSessionId(fuelSessionId);
        entry.setSource(source);
        entry.setReadingType(readingType);
        entry.setFuelLevelPct(fuelLevelPct);
        entry.setFuelLiters(fuelLiters);
        entry.setOdometerKm(odometerKm);
        entry.setLatitude(latitude);
        entry.setLongitude(longitude);
        entry.setDistanceDeltaKm(distanceDeltaKm);
        entry.setConsumedLiters(consumedLiters);
        entry.setRecordedAt(recordedAt);
        entry.setNotes(notes);
        entry.setEntityStatus(EntityStatus.ACTIVE);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setCreatedBy(createdBy);
        return entry;
    }
}
