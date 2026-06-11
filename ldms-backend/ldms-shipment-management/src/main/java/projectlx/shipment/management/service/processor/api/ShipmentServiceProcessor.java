package projectlx.shipment.management.service.processor.api;

import projectlx.shipment.management.utils.requests.AllocateShipmentRequest;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.UpdateShipmentStatusRequest;
import projectlx.shipment.management.utils.responses.ShipmentResponse;

import java.util.Locale;

public interface ShipmentServiceProcessor {

    ShipmentResponse findById(Long id, Locale locale, String username);

    ShipmentResponse findByMultipleFilters(ShipmentMultipleFiltersRequest request, Locale locale, String username);

    ShipmentResponse findByTransferId(Long transferId, Locale locale, String username);

    ShipmentResponse allocateFleet(AllocateShipmentRequest request, Locale locale, String username);

    ShipmentResponse updateStatus(UpdateShipmentStatusRequest request, Locale locale, String username);
}
