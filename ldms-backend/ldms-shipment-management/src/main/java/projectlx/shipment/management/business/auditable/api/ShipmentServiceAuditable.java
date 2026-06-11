package projectlx.shipment.management.business.auditable.api;

import projectlx.shipment.management.model.Shipment;

import java.util.Locale;

public interface ShipmentServiceAuditable {

    Shipment create(Shipment shipment, Locale locale, String username);

    Shipment update(Shipment shipment, Locale locale, String username);
}
