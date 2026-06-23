package projectlx.shipment.management.service.processor.api;

import projectlx.shipment.management.utils.responses.PlatformShipmentDashboardResponse;

import java.util.List;
import java.util.Locale;

public interface PlatformDashboardServiceProcessor {

    PlatformShipmentDashboardResponse getShipmentDashboard(Locale locale);

    PlatformShipmentDashboardResponse searchShipments(String term, List<Long> purchaseOrderIds, int limit, Locale locale);
}
