package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class CreatePurchaseOrderLineRequest {

    // Relationships
    private Long purchaseOrderId;
    private Long productId;

    // Details
    private UnitOfMeasure unitOfMeasure;

    // Updated to BigDecimal for consistency with the entity
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    // Receiving
    // Updated to BigDecimal for consistency with the entity
    private BigDecimal receivedQuantity;
}
