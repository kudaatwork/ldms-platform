package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class ReallocateInventoryRequest {
    private Long productId;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private BigDecimal quantity;
}
