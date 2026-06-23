package projectlx.fleet.management.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.fleet.management.business.logic.api.FleetAssetService;
import projectlx.fleet.management.business.logic.api.FleetComplianceService;
import projectlx.fleet.management.business.logic.api.FleetDriverService;
import projectlx.fleet.management.business.logic.api.FleetDriverSignupRequestService;
import projectlx.fleet.management.business.logic.api.FleetTrackingDeviceService;
import projectlx.fleet.management.business.logic.api.FleetTrackingIntegrationCredentialService;
import projectlx.fleet.management.business.logic.api.FleetDashboardService;
import projectlx.fleet.management.service.processor.api.FleetAssetServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetDashboardServiceProcessor;
import projectlx.fleet.management.service.processor.impl.FleetDashboardServiceProcessorImpl;
import projectlx.fleet.management.service.processor.api.FleetComplianceServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetDriverServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetDriverSignupRequestServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetTrackingDeviceServiceProcessor;
import projectlx.fleet.management.service.processor.api.FleetTrackingIntegrationCredentialServiceProcessor;
import projectlx.fleet.management.service.processor.impl.FleetAssetServiceProcessorImpl;
import projectlx.fleet.management.service.processor.impl.FleetComplianceServiceProcessorImpl;
import projectlx.fleet.management.service.processor.impl.FleetDriverServiceProcessorImpl;
import projectlx.fleet.management.service.processor.impl.FleetDriverSignupRequestServiceProcessorImpl;
import projectlx.fleet.management.service.processor.impl.FleetTrackingDeviceServiceProcessorImpl;
import projectlx.fleet.management.service.processor.impl.FleetTrackingIntegrationCredentialServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public FleetDashboardServiceProcessor fleetDashboardServiceProcessor(FleetDashboardService fleetDashboardService) {
        return new FleetDashboardServiceProcessorImpl(fleetDashboardService);
    }

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

    @Bean
    public FleetTrackingDeviceServiceProcessor fleetTrackingDeviceServiceProcessor(
            FleetTrackingDeviceService fleetTrackingDeviceService) {
        return new FleetTrackingDeviceServiceProcessorImpl(fleetTrackingDeviceService);
    }

    @Bean
    public FleetTrackingIntegrationCredentialServiceProcessor fleetTrackingIntegrationCredentialServiceProcessor(
            FleetTrackingIntegrationCredentialService fleetTrackingIntegrationCredentialService) {
        return new FleetTrackingIntegrationCredentialServiceProcessorImpl(fleetTrackingIntegrationCredentialService);
    }

    @Bean
    public FleetDriverSignupRequestServiceProcessor fleetDriverSignupRequestServiceProcessor(
            FleetDriverSignupRequestService fleetDriverSignupRequestService) {
        return new FleetDriverSignupRequestServiceProcessorImpl(fleetDriverSignupRequestService);
    }
}
