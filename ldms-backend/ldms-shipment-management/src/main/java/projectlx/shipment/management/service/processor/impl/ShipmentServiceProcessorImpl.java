package projectlx.shipment.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.shipment.management.business.logic.api.ShipmentService;
import projectlx.shipment.management.service.processor.api.ShipmentServiceProcessor;
import projectlx.shipment.management.utils.requests.AllocateShipmentRequest;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.UpdateShipmentStatusRequest;
import projectlx.shipment.management.utils.responses.ShipmentResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceProcessorImpl implements ShipmentServiceProcessor {

    private final ShipmentService shipmentService;

    @Override
    public ShipmentResponse findById(Long id, Locale locale, String username) {
        log.info("Processing find shipment by id={} for user={}", id, username);
        return shipmentService.findById(id, locale, username);
    }

    @Override
    public ShipmentResponse findByMultipleFilters(ShipmentMultipleFiltersRequest request, Locale locale, String username) {
        log.info("Processing find shipments by filters for user={}", username);
        return shipmentService.findByMultipleFilters(request, locale, username);
    }

    @Override
    public ShipmentResponse findByTransferId(Long transferId, Locale locale, String username) {
        log.info("Processing find shipment by transferId={} for user={}", transferId, username);
        return shipmentService.findByTransferId(transferId, locale, username);
    }

    @Override
    public ShipmentResponse allocateFleet(AllocateShipmentRequest request, Locale locale, String username) {
        log.info("Processing allocate fleet for shipmentId={} by user={}", request != null ? request.getShipmentId() : null, username);
        return shipmentService.allocateFleet(request, locale, username);
    }

    @Override
    public ShipmentResponse updateStatus(UpdateShipmentStatusRequest request, Locale locale, String username) {
        log.info("Processing update status for shipmentId={} to status={} by user={}",
                request != null ? request.getShipmentId() : null,
                request != null ? request.getStatus() : null, username);
        return shipmentService.updateStatus(request, locale, username);
    }
}
