package projectlx.trip.tracking.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import projectlx.trip.tracking.business.logic.api.PlatformDashboardService;
import projectlx.trip.tracking.business.logic.api.TripDeliveryService;
import projectlx.trip.tracking.business.logic.api.TripLiveService;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.trip.tracking.business.logic.api.TripTelemetryIngestService;
import projectlx.trip.tracking.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.trip.tracking.service.processor.api.TripDeliveryServiceProcessor;
import projectlx.trip.tracking.service.processor.api.TripLiveServiceProcessor;
import projectlx.trip.tracking.service.processor.api.TripServiceProcessor;
import projectlx.trip.tracking.service.processor.api.TripTelemetryIngestServiceProcessor;
import projectlx.trip.tracking.service.processor.impl.PlatformDashboardServiceProcessorImpl;
import projectlx.trip.tracking.service.processor.impl.TripDeliveryServiceProcessorImpl;
import projectlx.trip.tracking.service.processor.impl.TripLiveServiceProcessorImpl;
import projectlx.trip.tracking.service.processor.impl.TripServiceProcessorImpl;
import projectlx.trip.tracking.service.processor.impl.TripTelemetryIngestServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public PlatformDashboardServiceProcessor platformDashboardServiceProcessor(
            PlatformDashboardService platformDashboardService) {
        return new PlatformDashboardServiceProcessorImpl(platformDashboardService);
    }

    @Bean
    public TripServiceProcessor tripServiceProcessor(TripService tripService) {
        return new TripServiceProcessorImpl(tripService);
    }

    @Bean
    public TripLiveServiceProcessor tripLiveServiceProcessor(TripLiveService tripLiveService) {
        return new TripLiveServiceProcessorImpl(tripLiveService);
    }

    @Bean
    public TripTelemetryIngestServiceProcessor tripTelemetryIngestServiceProcessor(
            TripTelemetryIngestService tripTelemetryIngestService) {
        return new TripTelemetryIngestServiceProcessorImpl(tripTelemetryIngestService);
    }

    @Bean
    public TripDeliveryServiceProcessor tripDeliveryServiceProcessor(TripDeliveryService tripDeliveryService) {
        return new TripDeliveryServiceProcessorImpl(tripDeliveryService);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
