package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesReservationDto {

    private Long id;
    private String reservationNumber;
    private Long inventoryItemId;
    private BigDecimal quantityReserved;
    private ReservationStatus reservationStatus;
    private ReservationType reservedForType;
    private Long reservedForId;
    private Long reservedByUserId;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private String notes;

    private Long productId;
    private Long warehouseLocationId;
    private Long customerId;
    private LocalDateTime reservedUntil;
    private Long createdByUserId;

    // Computed fields
    private Boolean expired;
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;
}