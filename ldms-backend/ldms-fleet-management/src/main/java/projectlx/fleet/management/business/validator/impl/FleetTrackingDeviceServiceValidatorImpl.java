package projectlx.fleet.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fleet.management.business.validator.api.FleetTrackingDeviceServiceValidator;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.enums.TrackingDeviceType;
import projectlx.fleet.management.utils.enums.TrackingIntegrationProvider;
import projectlx.fleet.management.utils.requests.EditFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FleetTrackingDeviceServiceValidatorImpl implements FleetTrackingDeviceServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FleetTrackingDeviceServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isInstallRequestValid(InstallFleetTrackingDeviceRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: install tracking device request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getDeviceLabel() == null || request.getDeviceLabel().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"deviceLabel"}, locale));
        }
        if (request.getDeviceType() == null || request.getDeviceType().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"deviceType"}, locale));
        } else {
            try {
                TrackingDeviceType.valueOf(request.getDeviceType());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_TRACKING_DEVICE_TYPE_INVALID.getCode(),
                        new String[]{}, locale));
            }
        }
        if (request.getIntegrationProvider() != null && !request.getIntegrationProvider().isBlank()) {
            try {
                TrackingIntegrationProvider.valueOf(request.getIntegrationProvider());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_TRACKING_INTEGRATION_PROVIDER_INVALID.getCode(),
                        new String[]{}, locale));
            }
        }

        // Live trip tracking ingest requires the device to be bound to a specific vehicle asset.
        // Trip telemetry resolution uses fleet_asset_id to find an active IN_TRANSIT trip.
        boolean tracksGps = request.getTracksGps() == null || Boolean.TRUE.equals(request.getTracksGps());
        if (tracksGps) {
            if (request.getFleetAssetId() == null || request.getFleetAssetId() < 1) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                        new String[]{"fleetAssetId"}, locale));
            }
        }

        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>())
                : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isEditRequestValid(EditFleetTrackingDeviceRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: edit tracking device request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getId() == null || request.getId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"id"}, locale));
        }
        if (request.getDeviceLabel() == null || request.getDeviceLabel().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"deviceLabel"}, locale));
        }

        // If the device is configured to track GPS, it must remain bound to a vehicle asset.
        if (Boolean.TRUE.equals(request.getTracksGps())) {
            if (request.getFleetAssetId() == null || request.getFleetAssetId() < 1) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                        new String[]{"fleetAssetId"}, locale));
            }
        }

        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>())
                : new ValidatorDto(false, null, errors);
    }
}
