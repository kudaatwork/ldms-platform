package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class InventoryAvailabilityRequest {
    private Long productId;
    private Long warehouseId;
    private BigDecimal requiredQuantity; // optional
}
