package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditPurchaseOrderLineRequest {

    // Identifier
    private Long purchaseOrderLineId;

    // Editable fields
    private Long purchaseOrderId;
    private Long productId;
    private UnitOfMeasure unitOfMeasure;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal receivedQuantity;
    private EntityStatus entityStatus;

    // NEW AUDIT FIELD
    private Long updatedByUserId;
}
