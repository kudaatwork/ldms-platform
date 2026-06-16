package projectlx.trip.tracking.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.trip.tracking.utils.responses.FleetDriverFeignResponse;
import projectlx.trip.tracking.utils.responses.FleetTrackingDeviceFeignResponse;

import java.util.Locale;

public interface FleetManagementServiceClient {

    @GetMapping("/ldms-fleet-management/v1/system/fleet-driver/find-by-id/{id}")
    FleetDriverFeignResponse findFleetDriverById(
            @PathVariable("id") Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping("/ldms-fleet-management/v1/system/tracking-device/resolve-by-ingest-key/{ingestKey}")
    FleetTrackingDeviceFeignResponse resolveTrackingDeviceByIngestKey(
            @PathVariable("ingestKey") String ingestKey,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-fleet-management/v1/system/tracking-device/mark-telemetry/{id}")
    void markDeviceTelemetry(@PathVariable("id") Long id);
}
