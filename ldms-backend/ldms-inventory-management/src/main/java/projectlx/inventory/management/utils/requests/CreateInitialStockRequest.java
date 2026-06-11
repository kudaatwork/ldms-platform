package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInitialStockRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse Location ID is required")
    private Long warehouseLocationId;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.00", message = "Unit cost must be non-negative")
    private BigDecimal unitCost;

    // Optional inventory management fields
    @DecimalMin(value = "0.00", message = "Minimum stock level must be non-negative")
    private BigDecimal minStockLevel;

    @DecimalMin(value = "0.00", message = "Reorder quantity must be non-negative")
    private BigDecimal reorderQuantity;

    private String batchLot;

    private String serialNumber;

    private LocalDate expiresAt;

    private String notes;

    @NotNull(message = "Created by user ID is required")
    private Long createdByUserId;
}