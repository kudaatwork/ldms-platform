package projectlx.fleet.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fleet.management.business.logic.api.FleetTrackingDeviceService;
import projectlx.fleet.management.business.logic.api.FleetTrackingIntegrationCredentialService;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.FleetTrackingIntegrationCredentialMapper;
import projectlx.fleet.management.business.validator.api.FleetTrackingIntegrationCredentialServiceValidator;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.model.FleetTrackingDevice;
import projectlx.fleet.management.repository.FleetAssetRepository;
import projectlx.fleet.management.repository.FleetTrackingDeviceRepository;
import projectlx.fleet.management.utils.dtos.FleetTrackingIntegrationCredentialDto;
import projectlx.fleet.management.utils.enums.FleetRegistrationStatus;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.enums.TrackingDeviceType;
import projectlx.fleet.management.utils.enums.TrackingIntegrationProvider;
import projectlx.fleet.management.utils.requests.CreateFleetTrackingIntegrationCredentialRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingDeviceResponse;
import projectlx.fleet.management.utils.responses.FleetTrackingIntegrationCredentialResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class FleetTrackingIntegrationCredentialServiceImpl implements FleetTrackingIntegrationCredentialService {

    private final FleetTrackingIntegrationCredentialServiceValidator validator;
    private final FleetTrackingDeviceService fleetTrackingDeviceService;
    private final FleetTrackingDeviceRepository trackingDeviceRepository;
    private final FleetAssetRepository fleetAssetRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetTrackingIntegrationCredentialResponse create(
            CreateFleetTrackingIntegrationCredentialRequest request, Locale locale, String username) {

        ValidatorDto validation = validator.isCreateRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long callerOrgId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (callerOrgId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        if (!callerOrgId.equals(request.getOrganizationId())) {
            return errorResponse(403, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_ORG_MISMATCH.getCode(), new String[]{}, locale));
        }

        InstallFleetTrackingDeviceRequest installRequest = new InstallFleetTrackingDeviceRequest();
        installRequest.setDeviceLabel(request.getCredentialLabel());
        installRequest.setDeviceType(TrackingDeviceType.DEDICATED_GPS.name());
        installRequest.setIntegrationProvider(request.getIntegrationProvider());
        installRequest.setFleetAssetId(request.getFleetAssetId());
        installRequest.setExternalDeviceId(request.getExternalDeviceId());
        installRequest.setTracksGps(true);
        installRequest.setTracksFuel(false);
        installRequest.setNotes(request.getNotes());

        FleetTrackingDeviceResponse deviceResponse =
                fleetTrackingDeviceService.install(installRequest, locale, username);
        if (deviceResponse.getStatusCode() != 201 || deviceResponse.getFleetTrackingDeviceDto() == null) {
            FleetTrackingIntegrationCredentialResponse response = new FleetTrackingIntegrationCredentialResponse();
            response.setStatusCode(deviceResponse.getStatusCode());
            response.setMessage(deviceResponse.getMessage());
            response.setErrorMessages(deviceResponse.getErrorMessages());
            return response;
        }

        FleetTrackingIntegrationCredentialResponse response = successResponse(201,
                messageService.getMessage(
                        I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_CREATE_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingIntegrationCredentialDto(
                FleetTrackingIntegrationCredentialMapper.fromDeviceDto(deviceResponse.getFleetTrackingDeviceDto()));
        log.info("Tracking integration credential created id={} for org={} by {}",
                response.getFleetTrackingIntegrationCredentialDto().getId(), callerOrgId, username);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetTrackingIntegrationCredentialResponse findAllByOrganization(
            Long organizationId, Locale locale, String username) {

        ValidatorDto validation = validator.isOrganizationIdValid(organizationId, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long callerOrgId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (callerOrgId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        if (!callerOrgId.equals(organizationId)) {
            return errorResponse(403, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_ORG_MISMATCH.getCode(), new String[]{}, locale));
        }

        List<FleetTrackingDevice> devices = trackingDeviceRepository
                .findByOrganizationIdAndIntegrationProviderNotAndEntityStatusNotOrderByIdDesc(
                        organizationId, TrackingIntegrationProvider.LDMS_MOBILE, EntityStatus.DELETED);

        Map<Long, FleetAsset> assetMap = fleetAssetRepository
                .findByOrganizationIdAndRegistrationStatusAndEntityStatusNotOrderByIdDesc(
                        organizationId, FleetRegistrationStatus.ACTIVE, EntityStatus.DELETED)
                .stream()
                .collect(Collectors.toMap(FleetAsset::getId, a -> a));

        List<FleetTrackingIntegrationCredentialDto> dtoList = devices.stream()
                .map(d -> FleetTrackingIntegrationCredentialMapper.fromDevice(
                        d, assetMap.get(d.getFleetAssetId()), true))
                .toList();

        FleetTrackingIntegrationCredentialResponse response = successResponse(200,
                messageService.getMessage(
                        I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_LIST_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingIntegrationCredentialDtoList(dtoList);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetTrackingIntegrationCredentialResponse findById(Long id, Locale locale, String username) {
        ValidatorDto validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Optional<FleetTrackingDevice> deviceOpt = requireIntegratorDevice(id, username);
        if (deviceOpt.isEmpty()) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        FleetTrackingDevice device = deviceOpt.get();
        FleetAsset asset = resolveAsset(device);

        FleetTrackingIntegrationCredentialResponse response = successResponse(200,
                messageService.getMessage(
                        I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_LIST_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingIntegrationCredentialDto(
                FleetTrackingIntegrationCredentialMapper.fromDevice(device, asset, true));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetTrackingIntegrationCredentialResponse suspend(Long id, Locale locale, String username) {
        ValidatorDto validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        if (requireIntegratorDevice(id, username).isEmpty()) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        FleetTrackingDeviceResponse deviceResponse = fleetTrackingDeviceService.suspend(id, locale, username);
        if (deviceResponse.getStatusCode() != 200 || deviceResponse.getFleetTrackingDeviceDto() == null) {
            FleetTrackingIntegrationCredentialResponse response = new FleetTrackingIntegrationCredentialResponse();
            response.setStatusCode(deviceResponse.getStatusCode());
            response.setMessage(deviceResponse.getMessage());
            response.setErrorMessages(deviceResponse.getErrorMessages());
            return response;
        }

        FleetTrackingIntegrationCredentialResponse response = successResponse(200,
                messageService.getMessage(
                        I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_SUSPEND_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetTrackingIntegrationCredentialDto(
                FleetTrackingIntegrationCredentialMapper.fromDeviceDto(deviceResponse.getFleetTrackingDeviceDto()));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetTrackingIntegrationCredentialResponse delete(Long id, Locale locale, String username) {
        ValidatorDto validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        if (requireIntegratorDevice(id, username).isEmpty()) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        FleetTrackingDeviceResponse deviceResponse = fleetTrackingDeviceService.delete(id, locale, username);
        FleetTrackingIntegrationCredentialResponse response = new FleetTrackingIntegrationCredentialResponse();
        response.setStatusCode(deviceResponse.getStatusCode());
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_DELETE_SUCCESS.getCode(), new String[]{}, locale));
        if (deviceResponse.getStatusCode() != 200) {
            response.setMessage(deviceResponse.getMessage());
            response.setErrorMessages(deviceResponse.getErrorMessages());
        }
        return response;
    }

    private Optional<FleetTrackingDevice> requireIntegratorDevice(Long id, String username) {
        Long callerOrgId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (callerOrgId == null) {
            return Optional.empty();
        }
        Optional<FleetTrackingDevice> deviceOpt =
                trackingDeviceRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (deviceOpt.isEmpty()) {
            return Optional.empty();
        }
        FleetTrackingDevice device = deviceOpt.get();
        if (!callerOrgId.equals(device.getOrganizationId())) {
            return Optional.empty();
        }
        if (device.getIntegrationProvider() == TrackingIntegrationProvider.LDMS_MOBILE) {
            return Optional.empty();
        }
        return Optional.of(device);
    }

    private FleetAsset resolveAsset(FleetTrackingDevice device) {
        if (device.getFleetAssetId() == null) {
            return null;
        }
        return fleetAssetRepository
                .findByIdAndEntityStatusNot(device.getFleetAssetId(), EntityStatus.DELETED)
                .orElse(null);
    }

    private FleetTrackingIntegrationCredentialResponse successResponse(int statusCode, String message) {
        FleetTrackingIntegrationCredentialResponse response = new FleetTrackingIntegrationCredentialResponse();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }

    private FleetTrackingIntegrationCredentialResponse errorResponse(int statusCode, String message) {
        FleetTrackingIntegrationCredentialResponse response = new FleetTrackingIntegrationCredentialResponse();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }

    private FleetTrackingIntegrationCredentialResponse errorResponse(
            int statusCode, String message, List<String> errors) {
        FleetTrackingIntegrationCredentialResponse response = errorResponse(statusCode, message);
        response.setErrorMessages(errors);
        return response;
    }
}
