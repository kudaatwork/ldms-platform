package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.TransferStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;

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
    private Long updatedByUserId;
    private EntityStatus entityStatus;
}
