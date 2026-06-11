package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.TransferStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class InventoryTransferMultipleFiltersRequest extends MultipleFiltersRequest {

    private Long productId;
    private Long fromLocationId;
    private Long toLocationId;
    private TransferStatus status;
    private String reference;
    private EntityStatus entityStatus;
}
