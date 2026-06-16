package projectlx.fuel.expenses.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.fuel.expenses.business.logic.api.FuelSessionService;
import projectlx.fuel.expenses.business.logic.api.FuelTelemetryLogService;
import projectlx.fuel.expenses.business.logic.api.OperationalFundRequestService;
import projectlx.fuel.expenses.service.processor.api.FuelSessionServiceProcessor;
import projectlx.fuel.expenses.service.processor.api.FuelTelemetryLogServiceProcessor;
import projectlx.fuel.expenses.service.processor.api.OperationalFundRequestServiceProcessor;
import projectlx.fuel.expenses.service.processor.impl.FuelSessionServiceProcessorImpl;
import projectlx.fuel.expenses.service.processor.impl.FuelTelemetryLogServiceProcessorImpl;
import projectlx.fuel.expenses.service.processor.impl.OperationalFundRequestServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public FuelSessionServiceProcessor fuelSessionServiceProcessor(FuelSessionService fuelSessionService) {
        return new FuelSessionServiceProcessorImpl(fuelSessionService);
    }

    @Bean
    public OperationalFundRequestServiceProcessor operationalFundRequestServiceProcessor(
            OperationalFundRequestService operationalFundRequestService) {
        return new OperationalFundRequestServiceProcessorImpl(operationalFundRequestService);
    }

    @Bean
    public FuelTelemetryLogServiceProcessor fuelTelemetryLogServiceProcessor(
            FuelTelemetryLogService fuelTelemetryLogService) {
        return new FuelTelemetryLogServiceProcessorImpl(fuelTelemetryLogService);
    }
}
