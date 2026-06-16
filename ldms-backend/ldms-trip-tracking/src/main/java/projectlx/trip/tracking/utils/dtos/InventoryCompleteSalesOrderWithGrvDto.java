package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryCompleteSalesOrderWithGrvDto {

    private Long salesOrderId;
    private Long receivedByUserId;
    private String idempotencyKey;
}
