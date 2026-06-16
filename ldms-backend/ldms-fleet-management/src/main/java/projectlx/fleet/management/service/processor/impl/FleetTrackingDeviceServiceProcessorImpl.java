package projectlx.fleet.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fleet.management.business.logic.api.FleetTrackingDeviceService;
import projectlx.fleet.management.service.processor.api.FleetTrackingDeviceServiceProcessor;
import projectlx.fleet.management.utils.dtos.FleetTrackingDeviceDto;
import projectlx.fleet.management.utils.requests.EditFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingDeviceResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FleetTrackingDeviceServiceProcessorImpl implements FleetTrackingDeviceServiceProcessor {

    private final FleetTrackingDeviceService fleetTrackingDeviceService;

    @Override
    public FleetTrackingDeviceResponse list(Locale locale, String username) {
        log.info("Processing list tracking devices for user {}", username);
        return fleetTrackingDeviceService.list(locale, username);
    }

    @Override
    public FleetTrackingDeviceResponse install(InstallFleetTrackingDeviceRequest request,
                                               Locale locale, String username) {
        log.info("Processing install tracking device for user {}", username);
        return fleetTrackingDeviceService.install(request, locale, username);
    }

    @Override
    public FleetTrackingDeviceResponse update(Long id, EditFleetTrackingDeviceRequest request,
                                              Locale locale, String username) {
        log.info("Processing update tracking device id={} for user {}", id, username);
        return fleetTrackingDeviceService.update(id, request, locale, username);
    }

    @Override
    public FleetTrackingDeviceResponse suspend(Long id, Locale locale, String username) {
        log.info("Processing suspend tracking device id={} for user {}", id, username);
        return fleetTrackingDeviceService.suspend(id, locale, username);
    }

    @Override
    public FleetTrackingDeviceResponse delete(Long id, Locale locale, String username) {
        log.info("Processing delete tracking device id={} for user {}", id, username);
        return fleetTrackingDeviceService.delete(id, locale, username);
    }

    @Override
    public FleetTrackingDeviceDto resolveByIngestKey(String ingestKey, Locale locale) {
        log.info("Processing system resolveByIngestKey");
        return fleetTrackingDeviceService.resolveByIngestKey(ingestKey, locale);
    }

    @Override
    public void markTelemetryReceived(Long id) {
        log.debug("Processing markTelemetryReceived for device id={}", id);
        fleetTrackingDeviceService.markTelemetryReceived(id);
    }
}
