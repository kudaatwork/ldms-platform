package projectlx.shipment.management.service.processor.impl;

import projectlx.shipment.management.business.logic.api.PlatformDashboardService;
import projectlx.shipment.management.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.shipment.management.utils.responses.PlatformShipmentDashboardResponse;

import java.util.Locale;

public class PlatformDashboardServiceProcessorImpl implements PlatformDashboardServiceProcessor {

    private final PlatformDashboardService platformDashboardService;

    public PlatformDashboardServiceProcessorImpl(PlatformDashboardService platformDashboardService) {
        this.platformDashboardService = platformDashboardService;
    }

    @Override
    public PlatformShipmentDashboardResponse getShipmentDashboard(Locale locale) {
        return platformDashboardService.getShipmentDashboard(locale);
    }
}
