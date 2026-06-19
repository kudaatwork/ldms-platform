package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.TransferStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class CreateInventoryTransferRequest {

    // A unique, human-readable identifier for the transfer
    private String transferNumber;

    // Relationships
    private Long productId;
    private Long fromLocationId;
    private Long toLocationId;

    // Details
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private TransferStatus status;

    // External reference
    private String reference;

    /** When true, shipment will require border clearance documents before crossing. */
    private Boolean crossBorder;

    private Long createdByUserId;

    /**
     * Optional route stops for this transfer (ORIGIN, EN_ROUTE_DEPOT, DESTINATION).
     * When provided these are atomically persisted in logistics_route_stop
     * after the transfer record is saved.
     */
    private List<RouteStopRequest> routeStops = new ArrayList<>();

}
