package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InventoryCompleteWithGrvDto {

    private Long transferId;
    private Long receivedByUserId;
    private String idempotencyKey;
}
