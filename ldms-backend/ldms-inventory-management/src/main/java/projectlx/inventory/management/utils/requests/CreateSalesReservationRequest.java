package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.ReservationType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class CreateSalesReservationRequest {

    private Long inventoryItemId;  // Direct reference to inventory
    private BigDecimal quantityReserved;
    private ReservationType reservedForType;  // ORDER, QUOTE, MANUAL, etc.
    private Long reservedForId;  // ID of order/quote/etc
    private LocalDateTime expiresAt;
    private String notes;
    private Long reservedByUserId;

    // Convenience fields for backward compatibility or easier API usage
    private Long productId;
    private Long warehouseLocationId;
    private Long customerId;
    private LocalDateTime reservedUntil;
    private Long createdByUserId;
}