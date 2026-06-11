package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@ToString
public class EditStockAdjustmentRequest {

    // Identifier - required for updates
    @NotNull(message = "Stock adjustment ID is required")
    private Long stockAdjustmentId;

    // Editable fields only (based on service implementation)
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    private Long adjustedByUserId;

    private EntityStatus entityStatus;
}