package projectlx.trip.tracking.business.logic.api;

import projectlx.trip.tracking.utils.requests.IngestTelemetryRequest;
import projectlx.trip.tracking.utils.responses.IngestTelemetryResponse;

import java.math.BigDecimal;
import java.util.Locale;

public interface TripTelemetryIngestService {

    /** REST / HTTP telemetry ingest path — authenticated by ingest key in request body. */
    IngestTelemetryResponse ingest(IngestTelemetryRequest request, Locale locale);

    /**
     * MQTT ingest path — topic provides orgId/assetId context; no feign lookup by ingest key.
     * Called by MqttIotConfiguration when a GPS message arrives on the structured topic pattern.
     */
    void ingestFromAsset(Long organizationId, Long fleetAssetId,
                         BigDecimal latitude, BigDecimal longitude,
                         BigDecimal speedKmh, BigDecimal headingDeg);
}
