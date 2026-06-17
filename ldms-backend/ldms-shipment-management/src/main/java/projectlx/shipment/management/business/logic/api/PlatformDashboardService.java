package projectlx.shipment.management.business.logic.api;

import projectlx.shipment.management.utils.responses.PlatformShipmentDashboardResponse;

import java.util.Locale;

public interface PlatformDashboardService {

    PlatformShipmentDashboardResponse getShipmentDashboard(Locale locale);
}
