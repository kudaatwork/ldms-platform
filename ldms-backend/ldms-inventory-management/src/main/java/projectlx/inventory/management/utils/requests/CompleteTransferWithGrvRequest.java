package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request to complete an IN_TRANSIT inventory transfer and auto-create a
 * Goods Received Voucher at the destination warehouse (no Purchase Order required).
 */
@Getter
@Setter
@ToString
public class CompleteTransferWithGrvRequest {
    private Long transferId;
    private Long receivedByUserId;
    private String idempotencyKey;
}
