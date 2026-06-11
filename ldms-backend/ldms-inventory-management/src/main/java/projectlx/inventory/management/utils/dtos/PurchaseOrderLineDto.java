package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderLineDto {

    private Long id;

    private Long purchaseOrderId;
    private Long productId;

    // New/updated fields aligned with adjusted PurchaseOrderLine entity
    private String supplierProductCode;
    private UnitOfMeasure unitOfMeasure;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal unitPriceFunctional;
    private BigDecimal totalPriceFunctional;
    private Long exchangeRateSnapshotId;
    private BigDecimal receivedQuantity;

    private Long createdByUserId;
    private Long updatedByUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
