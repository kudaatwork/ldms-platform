package projectlx.shipment.management.business.logic.api;

import projectlx.shipment.management.utils.requests.AddBorderClearanceDocumentRequest;
import projectlx.shipment.management.utils.requests.BorderClearanceMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.ClearBorderCaseRequest;
import projectlx.shipment.management.utils.requests.RejectBorderCaseRequest;
import projectlx.shipment.management.utils.responses.BorderClearanceCaseResponse;

import java.util.Locale;

public interface BorderClearanceCaseService {

    BorderClearanceCaseResponse findById(Long id, Locale locale, String username);

    BorderClearanceCaseResponse findByMultipleFilters(BorderClearanceMultipleFiltersRequest request, Locale locale, String username);

    BorderClearanceCaseResponse addDocument(AddBorderClearanceDocumentRequest request, Locale locale, String username);

    BorderClearanceCaseResponse submit(Long id, Locale locale, String username);

    BorderClearanceCaseResponse clear(Long id, ClearBorderCaseRequest request, Locale locale, String username);

    BorderClearanceCaseResponse reject(Long id, RejectBorderCaseRequest request, Locale locale, String username);

    /**
     * Called internally by ShipmentService when a cross-border shipment is created.
     * Auto-creates a border clearance case in AWAITING_DOCUMENTS status.
     */
    void autoCreateForShipment(Long shipmentId, Long organizationId, Long inventoryTransferId,
                               Long salesOrderId, Locale locale, String username);

    /**
     * Links a trip to the border clearance case for the given shipment when a trip is assigned.
     */
    void linkTripId(Long shipmentId, Long tripId, Locale locale, String username);
}
