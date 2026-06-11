package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.ReservationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class SalesReservationMultipleFiltersRequest extends MultipleFiltersRequest {

    private String reservationNumber;
    private Long inventoryItemId;
    private ReservationStatus reservationStatus;
    private ReservationType reservedForType;
    private Long reservedForId;
    private Long reservedByUserId;
    private EntityStatus entityStatus;
    private String searchValue;

    // Convenience filters for easier querying
    private Long productId;
    private Long warehouseLocationId;
    private Long customerId;
}