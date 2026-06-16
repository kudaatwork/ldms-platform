package projectlx.fleet.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.fleet.management.service.processor.api.FleetTrackingDeviceServiceProcessor;
import projectlx.fleet.management.utils.dtos.FleetTrackingDeviceDto;

import java.util.Locale;

/**
 * System-only tracking device endpoints called by trip-tracking and MQTT ingest.
 * No JWT session-org check — callers authenticate via service-to-service credentials.
 */
@CrossOrigin
@RestController
@RequestMapping("/ldms-fleet-management/v1/system/tracking-device")
@Tag(name = "Fleet Tracking Device System Resource",
        description = "Internal tracking device lookup for system callers (trip-tracking, MQTT ingest)")
@RequiredArgsConstructor
public class FleetTrackingDeviceSystemResource {

    private static final Logger logger = LoggerFactory.getLogger(FleetTrackingDeviceSystemResource.class);

    private final FleetTrackingDeviceServiceProcessor fleetTrackingDeviceServiceProcessor;

    @Auditable(action = "SYSTEM_RESOLVE_TRACKING_DEVICE_BY_INGEST_KEY")
    @GetMapping("/resolve-by-ingest-key/{ingestKey}")
    @Operation(summary = "Resolve tracking device by ingest key (system)",
            description = "Returns tracking device details by ingest key. No organisation-workspace restriction. "
                    + "For internal service-to-service calls only (trip-tracking, MQTT bridge).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tracking device found"),
            @ApiResponse(responseCode = "404", description = "Tracking device not found for given ingest key")
    })
    public ResponseEntity<FleetTrackingDeviceDto> resolveByIngestKey(
            @PathVariable("ingestKey") final String ingestKey,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.info("System request to resolve tracking device by ingestKey");
        FleetTrackingDeviceDto dto = fleetTrackingDeviceServiceProcessor.resolveByIngestKey(ingestKey, locale);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @Auditable(action = "SYSTEM_MARK_TELEMETRY_RECEIVED")
    @PostMapping("/mark-telemetry/{id}")
    @Operation(summary = "Mark telemetry received (system)",
            description = "Updates last_telemetry_at on the tracking device. Called after every successful telemetry ingest.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Telemetry timestamp updated"),
            @ApiResponse(responseCode = "404", description = "Tracking device not found")
    })
    public ResponseEntity<Void> markTelemetry(@PathVariable("id") final Long id) {
        logger.debug("System request to mark telemetry received for device id={}", id);
        fleetTrackingDeviceServiceProcessor.markTelemetryReceived(id);
        return ResponseEntity.noContent().build();
    }
}
