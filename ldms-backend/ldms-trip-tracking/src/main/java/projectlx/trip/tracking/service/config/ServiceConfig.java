package projectlx.trip.tracking.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.trip.tracking.service.processor.api.TripServiceProcessor;
import projectlx.trip.tracking.service.processor.impl.TripServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public TripServiceProcessor tripServiceProcessor(TripService tripService) {
        return new TripServiceProcessorImpl(tripService);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
