package projectlx.shipment.management.business.logic.api;

import projectlx.shipment.management.utils.responses.PlatformShipmentDashboardResponse;

import java.util.Locale;
import java.util.List;

public interface PlatformDashboardService {

    PlatformShipmentDashboardResponse getShipmentDashboard(Locale locale);

    PlatformShipmentDashboardResponse searchShipments(String term, List<Long> purchaseOrderIds, int limit, Locale locale);
}
