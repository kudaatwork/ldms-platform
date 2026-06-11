package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReleaseReservationRequest {
    private Long reservationId;
    private Long updatedByUserId;
}
