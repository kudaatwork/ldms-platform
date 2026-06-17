package projectlx.shipment.management.service.processor.api;

import projectlx.shipment.management.utils.responses.PlatformShipmentDashboardResponse;

import java.util.Locale;

public interface PlatformDashboardServiceProcessor {

    PlatformShipmentDashboardResponse getShipmentDashboard(Locale locale);
}
