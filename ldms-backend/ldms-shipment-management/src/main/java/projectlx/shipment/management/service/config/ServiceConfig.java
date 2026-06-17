package projectlx.shipment.management.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.shipment.management.business.logic.api.BorderClearanceCaseService;
import projectlx.shipment.management.business.logic.api.PlatformDashboardService;
import projectlx.shipment.management.business.logic.api.ShipmentService;
import projectlx.shipment.management.service.processor.api.BorderClearanceCaseServiceProcessor;
import projectlx.shipment.management.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.shipment.management.service.processor.api.ShipmentServiceProcessor;
import projectlx.shipment.management.service.processor.impl.BorderClearanceCaseServiceProcessorImpl;
import projectlx.shipment.management.service.processor.impl.PlatformDashboardServiceProcessorImpl;
import projectlx.shipment.management.service.processor.impl.ShipmentServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public PlatformDashboardServiceProcessor platformDashboardServiceProcessor(
            PlatformDashboardService platformDashboardService) {
        return new PlatformDashboardServiceProcessorImpl(platformDashboardService);
    }

    @Bean
    public ShipmentServiceProcessor shipmentServiceProcessor(ShipmentService shipmentService) {
        return new ShipmentServiceProcessorImpl(shipmentService);
    }

    @Bean
    public BorderClearanceCaseServiceProcessor borderClearanceCaseServiceProcessor(
            BorderClearanceCaseService borderClearanceCaseService) {
        return new BorderClearanceCaseServiceProcessorImpl(borderClearanceCaseService);
    }
}
