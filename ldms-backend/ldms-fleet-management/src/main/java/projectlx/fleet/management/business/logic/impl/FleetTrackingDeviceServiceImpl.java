package projectlx.fleet.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.fleet.management.business.auditable.api.FleetTrackingDeviceServiceAuditable;
import projectlx.fleet.management.business.logic.api.FleetTrackingDeviceService;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.FleetMapper;
import projectlx.fleet.management.business.validator.api.FleetTrackingDeviceServiceValidator;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.model.FleetTrackingDevice;
import projectlx.fleet.management.repository.FleetAssetRepository;
import projectlx.fleet.management.repository.FleetTrackingDeviceRepository;
import projectlx.fleet.management.utils.dtos.FleetTrackingDeviceDto;
import projectlx.fleet.management.utils.enums.FleetRegistrationStatus;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.enums.TrackingDeviceType;
import projectlx.fleet.management.utils.enums.TrackingInstallStatus;
import projectlx.fleet.management.utils.enums.TrackingIntegrationProvider;
import projectlx.fleet.management.utils.requests.EditFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingDeviceResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class FleetTrackingDeviceServiceImpl implements FleetTrackingDeviceService {

    private final FleetTrackingDeviceServiceValidator validator;
    private final FleetTrackingDeviceServiceAuditable auditable;
    private final FleetTrackingDeviceRepository trackingDeviceRepository;
    private final FleetAssetRepository fleetAssetRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;

    // ================================================================
    // LIST
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public FleetTrackingDeviceResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        List<FleetTrackingDevice> devices = trackingDeviceRepository
                .findByOrganizationIdAndEntityStatusNotOrderByIdDesc(organizationId, EntityStatus.DELETED);

        // Build an assetId → asset lookup for denormalised vehicle fields
        Map<Long, FleetAsset> assetMap = fleetAssetRepository
                .findByOrganizationIdAndRegistrationStatusAndEntityStatusNotOrderByIdDesc(
                        organizationId, FleetRegistrationStatus.ACTIVE, EntityStatus.DELETED)
                .stream()
                .collect(Collectors.toMap(FleetAsset::getId, a -> a));

        FleetTrackingDeviceResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRACKING_DEVICE_LIST_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingDeviceDtoList(
                devices.stream()
                        .map(d -> FleetMapper.toTrackingDeviceDto(d, assetMap.get(d.getFleetAssetId())))
                        .toList());
        return response;
    }

    // ================================================================
    // INSTALL
    // ================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetTrackingDeviceResponse install(InstallFleetTrackingDeviceRequest request,
                                               Locale locale, String username) {
        // STEP 1 — Validate request fields
        ValidatorDto validation = validator.isInstallRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_DEVICE_INSTALL_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        // STEP 2 — Validate fleet_asset belongs to org (when provided)
        FleetAsset asset = null;
        if (request.getFleetAssetId() != null) {
            Optional<FleetAsset> assetOpt = fleetAssetRepository
                    .findByIdAndEntityStatusNot(request.getFleetAssetId(), EntityStatus.DELETED);
            if (assetOpt.isEmpty() || !organizationId.equals(assetOpt.get().getOrganizationId())) {
                return errorResponse(404, messageService.getMessage(
                        I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(), new String[]{}, locale));
            }
            asset = assetOpt.get();
        }

        // STEP 3 — Build and persist the device
        FleetTrackingDevice device = new FleetTrackingDevice();
        device.setOrganizationId(organizationId);
        device.setFleetAssetId(request.getFleetAssetId());
        device.setFleetDriverId(request.getFleetDriverId());
        device.setLinkedUserId(request.getLinkedUserId());
        device.setDeviceType(TrackingDeviceType.valueOf(request.getDeviceType()));
        device.setInstallStatus(TrackingInstallStatus.ACTIVE);
        device.setIntegrationProvider(
                request.getIntegrationProvider() != null && !request.getIntegrationProvider().isBlank()
                        ? TrackingIntegrationProvider.valueOf(request.getIntegrationProvider())
                        : TrackingIntegrationProvider.LDMS_MOBILE);
        device.setDeviceLabel(request.getDeviceLabel());
        device.setDeviceSerial(request.getDeviceSerial());
        device.setExternalDeviceId(request.getExternalDeviceId());
        device.setIngestKey(UUID.randomUUID().toString().replace("-", ""));
        device.setTracksGps(request.getTracksGps() == null || request.getTracksGps());
        device.setTracksFuel(Boolean.TRUE.equals(request.getTracksFuel()));
        device.setNotes(request.getNotes());
        device.setInstalledAt(LocalDateTime.now());
        device.setEntityStatus(EntityStatus.ACTIVE);
        device.setCreatedAt(LocalDateTime.now());
        device.setCreatedBy(username);

        // STEP 4 — Set MQTT topic when asset GPS tracking is enabled
        if (device.isTracksGps() && request.getFleetAssetId() != null) {
            device.setMqttTopic("ldms/iot/" + organizationId + "/" + request.getFleetAssetId() + "/gps");
        }

        FleetTrackingDevice saved = auditable.create(device, locale, username);
        log.info("Installed tracking device id={} ingestKey={} for org={} by {}",
                saved.getId(), saved.getIngestKey(), organizationId, username);

        FleetTrackingDeviceResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_TRACKING_DEVICE_INSTALL_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingDeviceDto(FleetMapper.toTrackingDeviceDto(saved, asset));
        return response;
    }

    // ================================================================
    // UPDATE
    // ================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetTrackingDeviceResponse update(Long id, EditFleetTrackingDeviceRequest request,
                                              Locale locale, String username) {
        ValidatorDto validation = validator.isEditRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_DEVICE_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Optional<FleetTrackingDevice> deviceOpt = trackingDeviceRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (deviceOpt.isEmpty() || !organizationId.equals(deviceOpt.get().getOrganizationId())) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_DEVICE_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        FleetAsset asset = null;
        if (request.getFleetAssetId() != null) {
            Optional<FleetAsset> assetOpt = fleetAssetRepository
                    .findByIdAndEntityStatusNot(request.getFleetAssetId(), EntityStatus.DELETED);
            if (assetOpt.isEmpty() || !organizationId.equals(assetOpt.get().getOrganizationId())) {
                return errorResponse(404, messageService.getMessage(
                        I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(), new String[]{}, locale));
            }
            asset = assetOpt.get();
        }

        FleetTrackingDevice device = deviceOpt.get();
        device.setDeviceLabel(request.getDeviceLabel());
        device.setFleetAssetId(request.getFleetAssetId());
        device.setFleetDriverId(request.getFleetDriverId());
        device.setLinkedUserId(request.getLinkedUserId());
        device.setDeviceSerial(request.getDeviceSerial());
        device.setExternalDeviceId(request.getExternalDeviceId());
        if (request.getTracksGps() != null) device.setTracksGps(request.getTracksGps());
        if (request.getTracksFuel() != null) device.setTracksFuel(request.getTracksFuel());
        if (request.getIntegrationProvider() != null && !request.getIntegrationProvider().isBlank()) {
            device.setIntegrationProvider(TrackingIntegrationProvider.valueOf(request.getIntegrationProvider()));
        }
        device.setNotes(request.getNotes());
        // Recompute MQTT topic if asset changed
        if (device.isTracksGps() && device.getFleetAssetId() != null) {
            device.setMqttTopic("ldms/iot/" + organizationId + "/" + device.getFleetAssetId() + "/gps");
        } else {
            device.setMqttTopic(null);
        }
        device.setModifiedAt(LocalDateTime.now());
        device.setModifiedBy(username);

        FleetTrackingDevice saved = auditable.update(device, locale, username);
        FleetTrackingDeviceResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRACKING_DEVICE_UPDATE_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingDeviceDto(FleetMapper.toTrackingDeviceDto(saved, asset));
        return response;
    }

    // ================================================================
    // SUSPEND
    // ================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetTrackingDeviceResponse suspend(Long id, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        Optional<FleetTrackingDevice> deviceOpt = trackingDeviceRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (deviceOpt.isEmpty() || !organizationId.equals(deviceOpt.get().getOrganizationId())) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_DEVICE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        FleetTrackingDevice device = deviceOpt.get();
        device.setInstallStatus(TrackingInstallStatus.SUSPENDED);
        device.setModifiedAt(LocalDateTime.now());
        device.setModifiedBy(username);
        auditable.update(device, locale, username);
        FleetTrackingDeviceResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRACKING_DEVICE_SUSPEND_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingDeviceDto(FleetMapper.toTrackingDeviceDto(device, null));
        return response;
    }

    // ================================================================
    // DELETE (soft)
    // ================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetTrackingDeviceResponse delete(Long id, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        Optional<FleetTrackingDevice> deviceOpt = trackingDeviceRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (deviceOpt.isEmpty() || !organizationId.equals(deviceOpt.get().getOrganizationId())) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_DEVICE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        FleetTrackingDevice device = deviceOpt.get();
        device.setEntityStatus(EntityStatus.DELETED);
        device.setModifiedAt(LocalDateTime.now());
        device.setModifiedBy(username);
        auditable.delete(device, locale);
        return successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_TRACKING_DEVICE_DELETE_SUCCESS.getCode(), new String[]{}, locale));
    }

    // ================================================================
    // FIND BY ID
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public FleetTrackingDeviceResponse findById(Long id, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        Optional<FleetTrackingDevice> deviceOpt = trackingDeviceRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (deviceOpt.isEmpty() || (organizationId != null && !organizationId.equals(deviceOpt.get().getOrganizationId()))) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_DEVICE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        FleetTrackingDeviceResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRACKING_DEVICE_LIST_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingDeviceDto(FleetMapper.toTrackingDeviceDto(deviceOpt.get(), null));
        return response;
    }

    // ================================================================
    // SYSTEM — resolveByIngestKey (no org check)
    // ================================================================

    @Override
    @Transactional(readOnly = true)
    public FleetTrackingDeviceDto resolveByIngestKey(String ingestKey, Locale locale) {
        Optional<FleetTrackingDevice> deviceOpt = trackingDeviceRepository
                .findByIngestKeyAndEntityStatusNot(ingestKey, EntityStatus.DELETED);
        return deviceOpt.map(d -> FleetMapper.toTrackingDeviceDto(d, null)).orElse(null);
    }

    // ================================================================
    // SYSTEM — markTelemetryReceived
    // ================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void markTelemetryReceived(Long id) {
        trackingDeviceRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .ifPresent(device -> {
                    device.setLastTelemetryAt(LocalDateTime.now());
                    trackingDeviceRepository.save(device);
                });
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private FleetTrackingDeviceResponse successResponse(int statusCode, String message) {
        FleetTrackingDeviceResponse response = new FleetTrackingDeviceResponse();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }

    private FleetTrackingDeviceResponse errorResponse(int statusCode, String message) {
        FleetTrackingDeviceResponse response = new FleetTrackingDeviceResponse();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }

    private FleetTrackingDeviceResponse errorResponse(int statusCode, String message, List<String> errors) {
        FleetTrackingDeviceResponse response = errorResponse(statusCode, message);
        response.setErrorMessages(errors);
        return response;
    }
}
