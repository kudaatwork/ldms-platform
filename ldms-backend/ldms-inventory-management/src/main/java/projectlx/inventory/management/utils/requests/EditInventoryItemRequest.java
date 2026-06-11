package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditInventoryItemRequest {

    private Long inventoryItemId; // Identifier field

    // Editable fields
    private Long productId;
    private Long warehouseLocationId;
    private Long supplierId;
    private BigDecimal currentStock;
    private BigDecimal minStockLevel;
    private BigDecimal reorderQuantity;
    private String batchLot;
    private String serialNumber;
    private LocalDate expiresAt;
    private EntityStatus entityStatus;

    private Long updatedByUserId; // New audit field
}
