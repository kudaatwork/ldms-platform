package projectlx.trip.tracking.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.trip.tracking.service.processor.api.TripTelemetryIngestServiceProcessor;
import projectlx.trip.tracking.utils.requests.IngestTelemetryRequest;
import projectlx.trip.tracking.utils.responses.IngestTelemetryResponse;

import java.util.Locale;

/**
 * Telemetry ingest endpoint — accepts GPS position pushes from tracking devices.
 * Authenticated via ingest key in the request body (no JWT required).
 */
@CrossOrigin
@RestController
@RequestMapping("/ldms-trip-tracking/v1/system/telemetry")
@Tag(name = "Trip Telemetry System Resource",
        description = "Telemetry ingest for tracking devices (mobile apps, OBD, MQTT bridge)")
@RequiredArgsConstructor
public class TripTelemetrySystemResource {

    private static final Logger logger = LoggerFactory.getLogger(TripTelemetrySystemResource.class);

    private final TripTelemetryIngestServiceProcessor tripTelemetryIngestServiceProcessor;

    @Auditable(action = "TELEMETRY_INGEST")
    @PostMapping("/ingest")
    @Operation(summary = "Ingest telemetry",
            description = "Accepts a GPS telemetry push from a tracking device identified by its ingest key. "
                    + "If an IN_TRANSIT trip is found for the device's asset, the route plan and live snapshot are updated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telemetry ingested and trip updated"),
            @ApiResponse(responseCode = "202", description = "Telemetry received; no active trip for this device"),
            @ApiResponse(responseCode = "400", description = "Invalid request or device not active"),
            @ApiResponse(responseCode = "401", description = "Invalid ingest key"),
            @ApiResponse(responseCode = "503", description = "Fleet management service unavailable")
    })
    public ResponseEntity<IngestTelemetryResponse> ingest(
            @RequestBody final IngestTelemetryRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                    defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        logger.debug("Telemetry ingest received");
        IngestTelemetryResponse response = tripTelemetryIngestServiceProcessor.ingest(request, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
