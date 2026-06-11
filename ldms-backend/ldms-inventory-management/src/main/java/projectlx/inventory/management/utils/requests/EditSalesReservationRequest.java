package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.ReservationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class EditSalesReservationRequest {

    private Long salesReservationId;
    private BigDecimal quantityReserved;
    private ReservationStatus reservationStatus;
    private ReservationType reservedForType;
    private Long reservedForId;
    private LocalDateTime expiresAt;
    private String notes;
    private Long updatedByUserId;
    private EntityStatus entityStatus;
    private LocalDateTime reservedUntil;
}