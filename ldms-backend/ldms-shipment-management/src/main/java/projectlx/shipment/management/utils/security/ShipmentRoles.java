package projectlx.shipment.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ShipmentRoles {

    VIEW_SHIPMENTS("VIEW_SHIPMENTS", "Views shipments"),
    ALLOCATE_SHIPMENT("ALLOCATE_SHIPMENT", "Allocates fleet to a shipment"),
    UPDATE_SHIPMENT_STATUS("UPDATE_SHIPMENT_STATUS", "Updates shipment status");

    private final String roleName;
    private final String description;

    @Override
    public String toString() {
        return roleName;
    }
}
