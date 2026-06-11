package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class CreateStockAdjustmentRequest {

    // Relationships
    @NotNull(message = "Inventory item ID is required")
    private Long inventoryItemId;

    // Details
    @NotNull(message = "Quantity delta is required")
    private BigDecimal quantityDelta;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    @NotNull(message = "Adjusted by user ID is required")
    private Long adjustedByUserId;

    // NEW: Unit cost for this adjustment (critical for opening stock and proper valuation)
    private BigDecimal unitCost;
}