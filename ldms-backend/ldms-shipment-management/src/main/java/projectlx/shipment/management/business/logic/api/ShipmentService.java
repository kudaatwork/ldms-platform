package projectlx.shipment.management.business.logic.api;

import projectlx.shipment.management.utils.requests.AllocateShipmentRequest;
import projectlx.shipment.management.utils.requests.AssignTransportCompanyRequest;
import projectlx.shipment.management.utils.requests.AutoAllocateShipmentFromFleetRequest;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.UpdateShipmentStatusRequest;
import projectlx.shipment.management.utils.responses.ShipmentResponse;

import java.util.Locale;
import java.util.Map;

public interface ShipmentService {

    /**
     * Idempotently creates a shipment from an inventory transfer approved event payload.
     * No-ops if a shipment already exists for the given inventoryTransferId.
     */
    void createFromTransferApprovedEvent(Map<String, Object> event, Locale locale);

    void createFromSalesOrderApprovedEvent(Map<String, Object> event, Locale locale);

    /**
     * Idempotently creates a shipment from a cross.dock.dispatch.created event payload.
     * No-ops if a shipment already exists for the given crossDockDispatchId.
     */
    void createFromCrossDockDispatchCreatedEvent(Map<String, Object> event, Locale locale);

    ShipmentResponse findById(Long id, Locale locale, String username);

    ShipmentResponse findByMultipleFilters(ShipmentMultipleFiltersRequest request, Locale locale, String username);

    ShipmentResponse findByTransferId(Long transferId, Locale locale, String username);

    ShipmentResponse findBySalesOrderId(Long salesOrderId, Locale locale, String username);

    ShipmentResponse assignTransportCompany(AssignTransportCompanyRequest request, Locale locale, String username);

    ShipmentResponse allocateFleet(AllocateShipmentRequest request, Locale locale, String username);

    ShipmentResponse autoAllocateFromFleet(AutoAllocateShipmentFromFleetRequest request, Locale locale, String username);

    ShipmentResponse updateStatus(UpdateShipmentStatusRequest request, Locale locale, String username);
}
