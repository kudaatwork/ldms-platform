package projectlx.co.zw.fleetmanagement.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetAssetService;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetComplianceService;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetDriverService;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetAssetServiceProcessor;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetComplianceServiceProcessor;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetDriverServiceProcessor;
import projectlx.co.zw.fleetmanagement.service.processor.impl.FleetAssetServiceProcessorImpl;
import projectlx.co.zw.fleetmanagement.service.processor.impl.FleetComplianceServiceProcessorImpl;
import projectlx.co.zw.fleetmanagement.service.processor.impl.FleetDriverServiceProcessorImpl;

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
