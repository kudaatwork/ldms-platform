package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.TransactionType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockTransactionHistoryDto {

    private Long id;

    private Long inventoryItemId;
    private TransactionType transactionType;
    private BigDecimal quantityChange;
    private BigDecimal unitCost;
    private LocalDateTime timestamp;
    private Long warehouseLocationId;

    private Long performedByUserId;
    private Long referenceDocumentId;
    private ReferenceDocumentType referenceDocumentType;

    private String reason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
