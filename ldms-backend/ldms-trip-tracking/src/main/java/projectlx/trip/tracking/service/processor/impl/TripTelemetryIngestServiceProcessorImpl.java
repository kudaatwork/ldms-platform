package projectlx.trip.tracking.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.trip.tracking.business.logic.api.TripTelemetryIngestService;
import projectlx.trip.tracking.service.processor.api.TripTelemetryIngestServiceProcessor;
import projectlx.trip.tracking.utils.requests.IngestTelemetryRequest;
import projectlx.trip.tracking.utils.responses.IngestTelemetryResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class TripTelemetryIngestServiceProcessorImpl implements TripTelemetryIngestServiceProcessor {

    private final TripTelemetryIngestService tripTelemetryIngestService;

    @Override
    public IngestTelemetryResponse ingest(IngestTelemetryRequest request, Locale locale) {
        log.info("Processing telemetry ingest request");
        return tripTelemetryIngestService.ingest(request, locale);
    }
}
