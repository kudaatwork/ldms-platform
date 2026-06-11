package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.utils.enums.StockLevelStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryItemDto {

    private Long id;

    private Long productId;
    private String productName;
    private String productCode;
    private String productBarcode;
    private String unitOfMeasure;
    private Long supplierId;
    private Long warehouseLocationId;
    private String warehouseLocationName;
    private BigDecimal currentStock;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private StockLevelStatus stockStatus;
    private BigDecimal totalCost;
    /** Unit cost recorded when stock was captured (falls back to average cost). */
    private BigDecimal unitCost;
    private BigDecimal minStockLevel;
    private BigDecimal reorderQuantity;
    private String batchLot;
    private String serialNumber;

    private Long createdByUserId;
    private Long updatedByUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate expiresAt;

    private EntityStatus entityStatus;
}
