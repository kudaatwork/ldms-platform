package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.dtos.FleetTrackingDeviceDto;
import projectlx.fleet.management.utils.requests.EditFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingDeviceResponse;

import java.util.Locale;

public interface FleetTrackingDeviceServiceProcessor {
    FleetTrackingDeviceResponse list(Locale locale, String username);
    FleetTrackingDeviceResponse install(InstallFleetTrackingDeviceRequest request, Locale locale, String username);
    FleetTrackingDeviceResponse update(Long id, EditFleetTrackingDeviceRequest request, Locale locale, String username);
    FleetTrackingDeviceResponse suspend(Long id, Locale locale, String username);
    FleetTrackingDeviceResponse delete(Long id, Locale locale, String username);

    // System-facing
    FleetTrackingDeviceDto resolveByIngestKey(String ingestKey, Locale locale);
    void markTelemetryReceived(Long id);
}
