package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class CreateInventoryItemRequest {

    // Relationships
    private Long productId;
    private Long warehouseLocationId;

    // External references
    private Long supplierId;
    private Long createdByUserId; // New audit field

    // Stock info
    private BigDecimal currentStock;
    private BigDecimal minStockLevel;
    private BigDecimal reorderQuantity;

    // Tracking info
    private String batchLot;
    private String serialNumber;
    private LocalDate expiresAt;

    /** Optional seed hints; valuation still uses averageCost */
    private BigDecimal unitCost;          // informational
    private BigDecimal lastPurchaseCost;  // informational
}
