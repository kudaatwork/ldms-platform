package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.TransferStatus;

import java.math.BigDecimal;

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
    private Long createdByUserId;

}
