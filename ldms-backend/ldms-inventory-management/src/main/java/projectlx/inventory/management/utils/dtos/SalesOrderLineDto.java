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
public class SalesOrderLineDto {

    private Long id;
    private Long salesOrderId;
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal fulfilledQuantity;
    private UnitOfMeasure unitOfMeasure;
    private Long createdByUserId;
    private Long updatedByUserId;

    // Computed fields
    private BigDecimal remainingQuantity;
    private Boolean fullyFulfilled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;
}