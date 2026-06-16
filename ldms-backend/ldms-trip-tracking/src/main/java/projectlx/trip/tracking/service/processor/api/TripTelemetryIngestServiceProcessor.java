package projectlx.trip.tracking.service.processor.api;

import projectlx.trip.tracking.utils.requests.IngestTelemetryRequest;
import projectlx.trip.tracking.utils.responses.IngestTelemetryResponse;

import java.util.Locale;

public interface TripTelemetryIngestServiceProcessor {
    IngestTelemetryResponse ingest(IngestTelemetryRequest request, Locale locale);
}
