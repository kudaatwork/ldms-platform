package projectlx.fleet.management.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.fleet.management.business.logic.api.FleetAssetService;
import projectlx.fleet.management.business.logic.api.FleetComplianceService;
import projectlx.fleet.management.business.logic.api.FleetDriverService;
import projectlx.fleet.management.service.processor.api.FleetAssetServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetComplianceServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetDriverServiceProcessor;
import projectlx.fleet.management.service.processor.impl.FleetAssetServiceProcessorImpl;
import projectlx.fleet.management.service.processor.impl.FleetComplianceServiceProcessorImpl;
import projectlx.fleet.management.service.processor.impl.FleetDriverServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public FleetAssetServiceProcessor fleetAssetServiceProcessor(FleetAssetService fleetAssetService) {
        return new FleetAssetServiceProcessorImpl(fleetAssetService);
    }

    @Bean
    public FleetDriverServiceProcessor fleetDriverServiceProcessor(FleetDriverService fleetDriverService) {
        return new FleetDriverServiceProcessorImpl(fleetDriverService);
    }

    @Bean
    public FleetComplianceServiceProcessor fleetComplianceServiceProcessor(FleetComplianceService fleetComplianceService) {
        return new FleetComplianceServiceProcessorImpl(fleetComplianceService);
    }
}
