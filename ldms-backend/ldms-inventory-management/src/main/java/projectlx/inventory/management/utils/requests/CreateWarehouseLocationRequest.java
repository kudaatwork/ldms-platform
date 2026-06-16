package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.WarehouseLocationType;

@Getter
@Setter
@ToString
public class CreateWarehouseLocationRequest {

    // Basic info
    private String line1; // Street address
    private String line2; // Additional address info
    private String postalCode; // Postal code
    private Long suburbId; // ID of the suburb in the Location Service
    private Long geoCoordinatesId; // ID of the geo coordinates in the Location Service

    private String name;
    private String description;

    // External references
    private Long supplierId;

    private Long branchId;

    private WarehouseLocationType warehouseType;
}
