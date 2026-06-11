package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.TransactionType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditStockTransactionHistoryRequest {

    // Identifier
    private Long stockTransactionHistoryId;

    // Editable fields
    private Long inventoryItemId;
    private Long warehouseLocationId;
    private TransactionType transactionType;
    private BigDecimal quantityChange;
    private String reason;
    private LocalDateTime timestamp;
    private EntityStatus entityStatus;

    // NEW AUDIT FIELD
    private Long updatedByUserId;
}
