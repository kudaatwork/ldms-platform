package projectlx.fleet.management.business.logic.api;

import projectlx.fleet.management.utils.dtos.FleetTrackingDeviceDto;
import projectlx.fleet.management.utils.requests.EditFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.requests.InstallFleetTrackingDeviceRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingDeviceResponse;

import java.util.Locale;

public interface FleetTrackingDeviceService {
    FleetTrackingDeviceResponse list(Locale locale, String username);
    FleetTrackingDeviceResponse install(InstallFleetTrackingDeviceRequest request, Locale locale, String username);
    FleetTrackingDeviceResponse update(Long id, EditFleetTrackingDeviceRequest request, Locale locale, String username);
    FleetTrackingDeviceResponse suspend(Long id, Locale locale, String username);
    FleetTrackingDeviceResponse delete(Long id, Locale locale, String username);
    FleetTrackingDeviceResponse findById(Long id, Locale locale, String username);

    // System-only: no org check
    FleetTrackingDeviceDto resolveByIngestKey(String ingestKey, Locale locale);
    void markTelemetryReceived(Long id);
}
