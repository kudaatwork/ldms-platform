package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class CreateStockTransactionHistoryRequest {

    // Relationships
    private Long inventoryItemId;
    private Long warehouseLocationId;

    // NEW FIELDS for improved auditability
    private Long performedByUserId;
    private Long referenceDocumentId;
    private ReferenceDocumentType referenceDocumentType;

    // Details
    private TransactionType transactionType;
    private BigDecimal quantityChange; // Updated to BigDecimal
    private String reason;
    private LocalDateTime timestamp;
}
