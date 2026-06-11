package projectlx.shipment.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.shipment.management.business.auditable.api.ShipmentServiceAuditable;
import projectlx.shipment.management.model.Shipment;
import projectlx.shipment.management.repository.ShipmentRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class ShipmentServiceAuditableImpl implements ShipmentServiceAuditable {

    private final ShipmentRepository shipmentRepository;

    @Override
    public Shipment create(Shipment shipment, Locale locale, String username) {
        return shipmentRepository.save(shipment);
    }

    @Override
    public Shipment update(Shipment shipment, Locale locale, String username) {
        return shipmentRepository.save(shipment);
    }
}
