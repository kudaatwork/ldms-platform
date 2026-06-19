package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.TransferStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditInventoryTransferRequest {

    // Identifier
    private Long inventoryTransferId;

    // Editable fields
    private Long productId;
    private Long fromLocationId;
    private Long toLocationId;
    private BigDecimal quantity;
    private TransferStatus status;
    private String reference;

    /** When true, shipment will require border clearance after approval. */
    private Boolean crossBorder;

    private Long updatedByUserId;
    private EntityStatus entityStatus;

    /**
     * Optional route stops to replace on this transfer. When provided, replaces
     * all existing route stops atomically. Leave null/empty to leave stops unchanged.
     */
    private List<RouteStopRequest> routeStops = new ArrayList<>();
}
